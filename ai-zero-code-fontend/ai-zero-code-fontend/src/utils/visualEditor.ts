const VISUAL_EDITOR_MESSAGE_TYPE = 'ai-zero-code:visual-editor:element-selected'
const VISUAL_EDITOR_STYLE_ATTRIBUTE = 'data-ai-zero-code-visual-editor-style'
const VISUAL_EDITOR_HOVER_ATTRIBUTE = 'data-ai-zero-code-visual-editor-hover'
const VISUAL_EDITOR_SELECTED_ATTRIBUTE = 'data-ai-zero-code-visual-editor-selected'
const VISUAL_EDITOR_BRIDGE_KEY = '__aiZeroCodeVisualEditorPostMessage__'
const VISUAL_EDIT_CONTEXT_START = '--- AI_ZERO_CODE_VISUAL_EDIT_CONTEXT_START ---'
const VISUAL_EDIT_CONTEXT_END = '--- AI_ZERO_CODE_VISUAL_EDIT_CONTEXT_END ---'

const TEXT_CONTENT_LIMIT = 160
const ATTRIBUTE_VALUE_LIMIT = 160
const MAX_CLASS_NAMES = 8
const MAX_SELECTOR_DEPTH = 10

const RELEVANT_ATTRIBUTES = [
  'href',
  'src',
  'alt',
  'title',
  'placeholder',
  'name',
  'type',
  'role',
  'aria-label',
] as const

export type VisualEditorElementInfo = {
  tagName: string
  selector: string
  id?: string
  classNames: string[]
  textContent?: string
  attributes: Record<string, string>
}

export type VisualEditorOptions = {
  onElementSelected: (element: VisualEditorElementInfo) => void
}

export type VisualEditorController = {
  attach: (iframe: HTMLIFrameElement) => boolean
  setEnabled: (enabled: boolean) => boolean
  clearSelection: () => void
  destroy: () => void
}

type VisualEditorMessage = {
  type: typeof VISUAL_EDITOR_MESSAGE_TYPE
  payload: VisualEditorElementInfo
}

type VisualEditorFrameWindow = Window & {
  [VISUAL_EDITOR_BRIDGE_KEY]?: (payload: VisualEditorMessage, targetOrigin: string) => void
}

const normalizeText = (value: string | null | undefined, limit: number) => {
  if (!value) return ''
  const normalized = value.replace(/\s+/g, ' ').trim()
  if (normalized.length <= limit) return normalized
  return `${normalized.slice(0, limit - 1)}…`
}

const escapeCssIdentifier = (value: string) => {
  if (typeof CSS !== 'undefined' && typeof CSS.escape === 'function') {
    return CSS.escape(value)
  }
  return value.replace(/(^-?\d)|[^a-zA-Z0-9_-]/g, (match, leadingDigit: string) =>
    leadingDigit ? `\\3${leadingDigit} ` : `\\${match}`,
  )
}

const createSelectorSegment = (element: Element) => {
  if (element.id) return `#${escapeCssIdentifier(element.id)}`

  const tagName = element.tagName.toLowerCase()
  const parent = element.parentElement
  if (!parent) return tagName

  const sameTagSiblings = Array.from(parent.children).filter(
    (sibling) => sibling.tagName === element.tagName,
  )
  if (sameTagSiblings.length <= 1) return tagName

  const position = sameTagSiblings.indexOf(element) + 1
  return `${tagName}:nth-of-type(${position})`
}

const createElementSelector = (element: Element, document: Document) => {
  const segments: string[] = []
  let current: Element | null = element

  while (current && segments.length < MAX_SELECTOR_DEPTH) {
    const segment = createSelectorSegment(current)
    segments.unshift(segment)

    if (current.id || current === document.body || current === document.documentElement) break
    current = current.parentElement
  }

  return segments.join(' > ')
}

const getElementInfo = (element: Element, document: Document): VisualEditorElementInfo => {
  const attributes: Record<string, string> = {}
  for (const name of RELEVANT_ATTRIBUTES) {
    const value = normalizeText(element.getAttribute(name), ATTRIBUTE_VALUE_LIMIT)
    if (value) attributes[name] = value
  }

  const classNames = Array.from(element.classList).filter(Boolean).slice(0, MAX_CLASS_NAMES)
  const textContent = normalizeText(element.textContent, TEXT_CONTENT_LIMIT)

  return {
    tagName: element.tagName.toLowerCase(),
    selector: createElementSelector(element, document),
    id: element.id || undefined,
    classNames,
    textContent: textContent || undefined,
    attributes,
  }
}

const isVisualEditorMessage = (value: unknown): value is VisualEditorMessage => {
  if (!value || typeof value !== 'object') return false
  const message = value as Partial<VisualEditorMessage>
  const payload = message.payload as Partial<VisualEditorElementInfo> | undefined
  return (
    message.type === VISUAL_EDITOR_MESSAGE_TYPE &&
    Boolean(payload) &&
    typeof payload?.tagName === 'string' &&
    typeof payload.selector === 'string' &&
    Array.isArray(payload.classNames) &&
    Boolean(payload.attributes) &&
    typeof payload.attributes === 'object'
  )
}

export const buildVisualEditPrompt = (message: string, element: VisualEditorElementInfo) => {
  const value = message.trim()
  return `${value}\n\n${VISUAL_EDIT_CONTEXT_START}\n用户通过可视化编辑器选中了以下页面元素。请优先定位并修改该元素，并结合用户需求判断是否需要同步调整相关样式或结构。\n${JSON.stringify(
    element,
    null,
    2,
  )}\n${VISUAL_EDIT_CONTEXT_END}`
}

export const stripVisualEditContext = (message: string) => {
  const markerIndex = message.indexOf(`\n\n${VISUAL_EDIT_CONTEXT_START}`)
  if (markerIndex < 0) return message

  const endMarkerIndex = message.indexOf(VISUAL_EDIT_CONTEXT_END, markerIndex)
  if (endMarkerIndex < 0) return message

  return message.slice(0, markerIndex).trimEnd()
}

export const createVisualEditor = (options: VisualEditorOptions): VisualEditorController => {
  let iframe: HTMLIFrameElement | null = null
  let frameWindow: Window | null = null
  let frameDocument: Document | null = null
  let styleElement: HTMLStyleElement | null = null
  let postMessageToParent: VisualEditorFrameWindow[typeof VISUAL_EDITOR_BRIDGE_KEY] | null = null
  let hoveredElement: Element | null = null
  let selectedElement: Element | null = null
  let enabled = false

  const clearHover = () => {
    hoveredElement?.removeAttribute(VISUAL_EDITOR_HOVER_ATTRIBUTE)
    hoveredElement = null
  }

  const clearSelection = () => {
    selectedElement?.removeAttribute(VISUAL_EDITOR_SELECTED_ATTRIBUTE)
    selectedElement = null
  }

  const clearEditorAttributes = () => {
    if (!frameDocument) return
    frameDocument
      .querySelectorAll(`[${VISUAL_EDITOR_HOVER_ATTRIBUTE}], [${VISUAL_EDITOR_SELECTED_ATTRIBUTE}]`)
      .forEach((element) => {
        element.removeAttribute(VISUAL_EDITOR_HOVER_ATTRIBUTE)
        element.removeAttribute(VISUAL_EDITOR_SELECTED_ATTRIBUTE)
      })
    hoveredElement = null
    selectedElement = null
  }

  const getEventElement = (event: Event): Element | null => {
    const target = event.target
    if (!target || !frameDocument) return null

    const element = target as Element
    if (element.nodeType !== 1 || element.ownerDocument !== frameDocument) return null
    if (element === styleElement) return null
    return element
  }

  const handleMouseOver = (event: Event) => {
    if (!enabled) return
    const element = getEventElement(event)
    if (!element || element === hoveredElement) return

    clearHover()
    hoveredElement = element
    element.setAttribute(VISUAL_EDITOR_HOVER_ATTRIBUTE, '')
  }

  const handleMouseOut = (event: Event) => {
    if (!enabled) return
    const element = getEventElement(event)
    if (element && element === hoveredElement) clearHover()
  }

  const handleClick = (event: Event) => {
    if (!enabled || !frameWindow || !frameDocument) return
    const element = getEventElement(event)
    if (!element) return

    event.preventDefault()
    event.stopPropagation()
    event.stopImmediatePropagation()

    clearSelection()
    selectedElement = element
    element.setAttribute(VISUAL_EDITOR_SELECTED_ATTRIBUTE, '')

    const payload: VisualEditorMessage = {
      type: VISUAL_EDITOR_MESSAGE_TYPE,
      payload: getElementInfo(element, frameDocument),
    }
    postMessageToParent?.(payload, window.location.origin)
  }

  const detachFrame = () => {
    if (frameDocument) {
      frameDocument.removeEventListener('mouseover', handleMouseOver, true)
      frameDocument.removeEventListener('mouseout', handleMouseOut, true)
      frameDocument.removeEventListener('click', handleClick, true)
    }
    clearEditorAttributes()
    styleElement?.remove()
    styleElement = null
    if (frameWindow) {
      try {
        delete (frameWindow as VisualEditorFrameWindow)[VISUAL_EDITOR_BRIDGE_KEY]
      } catch {
        // iframe 可能已经在清理前跳转为其他源，此时忽略桥接函数清理失败。
      }
    }
    postMessageToParent = null
    frameDocument = null
    frameWindow = null
  }

  const attach = (nextIframe: HTMLIFrameElement) => {
    detachFrame()
    iframe = nextIframe

    try {
      const nextWindow = nextIframe.contentWindow
      const nextDocument = nextIframe.contentDocument
      if (!nextWindow || !nextDocument || nextWindow.location.origin !== window.location.origin) {
        return false
      }

      frameWindow = nextWindow
      frameDocument = nextDocument

      const bridgeScript = nextDocument.createElement('script')
      bridgeScript.textContent = `window.${VISUAL_EDITOR_BRIDGE_KEY} = function (payload, targetOrigin) { window.parent.postMessage(payload, targetOrigin); };`
      const bridgeContainer = nextDocument.head || nextDocument.documentElement
      bridgeContainer.appendChild(bridgeScript)
      bridgeScript.remove()
      postMessageToParent = (nextWindow as VisualEditorFrameWindow)[VISUAL_EDITOR_BRIDGE_KEY]
      if (typeof postMessageToParent !== 'function') {
        detachFrame()
        return false
      }

      styleElement = nextDocument.createElement('style')
      styleElement.setAttribute(VISUAL_EDITOR_STYLE_ATTRIBUTE, '')
      styleElement.textContent = `
        [${VISUAL_EDITOR_HOVER_ATTRIBUTE}] {
          outline: 2px solid #69b1ff !important;
          outline-offset: -2px !important;
          cursor: crosshair !important;
        }
        [${VISUAL_EDITOR_SELECTED_ATTRIBUTE}] {
          outline: 3px solid #1677ff !important;
          outline-offset: -3px !important;
          box-shadow: inset 0 0 0 1px rgba(22, 119, 255, 0.22) !important;
        }
      `
      nextDocument.head?.appendChild(styleElement)
      nextDocument.addEventListener('mouseover', handleMouseOver, true)
      nextDocument.addEventListener('mouseout', handleMouseOut, true)
      nextDocument.addEventListener('click', handleClick, true)
      return true
    } catch {
      detachFrame()
      return false
    }
  }

  const setEnabled = (nextEnabled: boolean) => {
    if (nextEnabled && (!iframe || !frameWindow || !frameDocument)) {
      enabled = false
      return false
    }

    enabled = nextEnabled
    if (!enabled) {
      clearHover()
      clearSelection()
    }
    return true
  }

  const handleWindowMessage = (event: MessageEvent<unknown>) => {
    if (
      event.origin !== window.location.origin ||
      !iframe ||
      event.source !== iframe.contentWindow ||
      !isVisualEditorMessage(event.data)
    ) {
      return
    }
    options.onElementSelected(event.data.payload)
  }

  window.addEventListener('message', handleWindowMessage)

  return {
    attach,
    setEnabled,
    clearSelection,
    destroy() {
      enabled = false
      detachFrame()
      iframe = null
      window.removeEventListener('message', handleWindowMessage)
    },
  }
}
