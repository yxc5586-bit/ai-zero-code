<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'

import { deployApp, downloadAppCode, getAppVoById } from '@/api/appController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { useLoginUserStore } from '@/stores/loginUser'
import type { ChatMessage } from '@/types/chat'
import { getApiErrorMessage, toSafePageNumber } from '@/utils/api'
import { formatCodeGenType, getStaticPreviewUrl, openExternalUrl } from '@/utils/format'
import { openCodeGenerationStream, type CodeGenerationStream } from '@/utils/sse'
import {
  buildVisualEditPrompt,
  createVisualEditor,
  stripVisualEditContext,
  type VisualEditorElementInfo,
} from '@/utils/visualEditor'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const appId = computed(() => String(route.params.id ?? ''))
const appInfo = ref<API.AppVO | null>(null)
const loading = ref(true)
const loadError = ref('')
const inputMessage = ref('')
const messages = ref<ChatMessage[]>([])
const generating = ref(false)
const downloading = ref(false)
const deploying = ref(false)
const previewReady = ref(false)
const previewVersion = ref(Date.now())
const mobilePanel = ref<'chat' | 'preview'>('chat')
const messageListRef = ref<HTMLElement | null>(null)
const showScrollButton = ref(false)
const historyLoading = ref(false)
const hasMoreHistory = ref(false)
const deployModalVisible = ref(false)
const deployedUrl = ref('')
const previewIframeRef = ref<HTMLIFrameElement | null>(null)
const isVisualEditing = ref(false)
const selectedElement = ref<VisualEditorElementInfo | null>(null)
let activeStream: CodeGenerationStream | null = null

const HISTORY_PAGE_SIZE = 10

const isOwner = computed(() =>
  Boolean(appInfo.value?.userId && appInfo.value.userId === loginUserStore.loginUser?.id),
)
const previewBaseUrl = computed(() => (appInfo.value ? getStaticPreviewUrl(appInfo.value) : ''))
const previewUrl = computed(() =>
  previewBaseUrl.value ? `${previewBaseUrl.value}?preview=${previewVersion.value}` : '',
)
const selectedElementLabel = computed(() => {
  const element = selectedElement.value
  if (!element) return ''
  if (element.id) return `<${element.tagName}#${element.id}>`
  if (element.classNames[0]) return `<${element.tagName}.${element.classNames[0]}>`
  return `<${element.tagName}>`
})

const visualEditor = createVisualEditor({
  onElementSelected(element) {
    selectedElement.value = element
    mobilePanel.value = 'chat'
  },
})

const createMessageId = () => `${Date.now()}-${Math.random().toString(36).slice(2)}`

const mapHistoryMessage = (record: API.ChatHistory): ChatMessage => {
  const role = record.messageType?.toLowerCase() === 'ai' ? 'assistant' : 'user'
  const content = record.message || ''
  return {
    id: record.id ? `history-${record.id}` : `history-${record.createTime}-${createMessageId()}`,
    role,
    content: role === 'user' ? stripVisualEditContext(content) : content,
    status: 'done',
    createTime: record.createTime,
  }
}

const sortMessagesByCreateTime = (items: ChatMessage[]) =>
  [...items].sort((left, right) => {
    const leftTime = left.createTime ? new Date(left.createTime).getTime() : 0
    const rightTime = right.createTime ? new Date(right.createTime).getTime() : 0
    if (leftTime !== rightTime) return leftTime - rightTime
    return left.id.localeCompare(right.id)
  })

const loadChatHistory = async (initial = false) => {
  if (historyLoading.value || (!initial && !hasMoreHistory.value)) return

  const cursor = initial ? undefined : messages.value.find((item) => item.createTime)?.createTime
  if (!initial && !cursor) {
    hasMoreHistory.value = false
    return
  }

  const container = messageListRef.value
  const previousScrollHeight = container?.scrollHeight ?? 0
  const previousScrollTop = container?.scrollTop ?? 0
  historyLoading.value = true

  try {
    const response = await listAppChatHistory({
      appId: appId.value,
      pageSize: HISTORY_PAGE_SIZE,
      lastCreateTime: cursor,
    })
    if (response.data.code !== 0) {
      throw new Error(response.data.message || '对话历史加载失败')
    }

    const pageData = response.data.data
    const historyRecords = pageData?.records ?? []
    const pageMessages = sortMessagesByCreateTime(historyRecords.map(mapHistoryMessage))
    const existingIds = new Set(messages.value.map((item) => item.id))
    const uniqueMessages = pageMessages.filter((item) => !existingIds.has(item.id))

    messages.value = initial ? uniqueMessages : [...uniqueMessages, ...messages.value]

    const remainingTotal = toSafePageNumber(pageData?.totalRow, -1)
    hasMoreHistory.value =
      remainingTotal >= 0
        ? remainingTotal > historyRecords.length
        : historyRecords.length === HISTORY_PAGE_SIZE

    if (initial) {
      previewReady.value = historyRecords.length >= 2
      await scrollToBottom(true)
    } else {
      await nextTick()
      if (container) {
        container.scrollTop = previousScrollTop + container.scrollHeight - previousScrollHeight
      }
    }
  } catch (error) {
    if (initial) throw error
    message.error(getApiErrorMessage(error, '更早的对话历史加载失败'))
  } finally {
    historyLoading.value = false
  }
}

const scrollToBottom = async (force = false) => {
  await nextTick()
  const container = messageListRef.value
  if (!container) return

  const distance = container.scrollHeight - container.scrollTop - container.clientHeight
  if (force || distance < 140) {
    container.scrollTo({ top: container.scrollHeight, behavior: 'smooth' })
    showScrollButton.value = false
  }
}

const handleMessageScroll = () => {
  const container = messageListRef.value
  if (!container) return
  showScrollButton.value =
    container.scrollHeight - container.scrollTop - container.clientHeight > 180
}

const clearSelectedElement = () => {
  selectedElement.value = null
  visualEditor.clearSelection()
}

const resetVisualEditing = () => {
  isVisualEditing.value = false
  selectedElement.value = null
  visualEditor.setEnabled(false)
}

const startGeneration = async (content: string, requestContent = content) => {
  const value = content.trim()
  const requestValue = requestContent.trim()
  if (!value || !requestValue || generating.value || !isOwner.value) return

  const userMessage: ChatMessage = {
    id: createMessageId(),
    role: 'user',
    content: value,
    status: 'done',
  }
  // 必须保留响应式代理。若继续修改 push 前的普通对象，Vue 不会在每个 SSE
  // 数据块到达时触发视图更新，只会在其他响应式状态变化后一次性显示完整内容。
  const assistantMessage = reactive<ChatMessage>({
    id: createMessageId(),
    role: 'assistant',
    content: '',
    status: 'streaming',
    sourceMessage: value,
    sourceRequestMessage: requestValue,
  })

  messages.value.push(userMessage, assistantMessage)
  inputMessage.value = ''
  resetVisualEditing()
  generating.value = true
  mobilePanel.value = 'chat'
  await scrollToBottom(true)

  activeStream?.close()
  activeStream = openCodeGenerationStream({
    appId: appId.value,
    message: requestValue,
    onChunk(chunk) {
      assistantMessage.content += chunk
      void scrollToBottom()
    },
    onDone() {
      activeStream = null
      assistantMessage.status = 'done'
      if (!assistantMessage.content) assistantMessage.content = '网站代码已生成完成。'
      generating.value = false
      previewReady.value = true
      previewVersion.value = Date.now()
      void scrollToBottom()
      message.success('网站生成完成，预览已刷新')
    },
    onError(error) {
      assistantMessage.status = 'error'
      assistantMessage.content ||= error.message
      generating.value = false
      activeStream = null
      message.error(error.message)
    },
  })
}

const sendCurrentMessage = () => {
  const content = inputMessage.value.trim()
  const requestContent = selectedElement.value
    ? buildVisualEditPrompt(content, selectedElement.value)
    : content
  void startGeneration(content, requestContent)
}

const handleInputKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendCurrentMessage()
  }
}

const retryMessage = (item: ChatMessage) => {
  if (item.sourceMessage) {
    void startGeneration(item.sourceMessage, item.sourceRequestMessage || item.sourceMessage)
  }
}

const refreshPreview = () => {
  if (!previewReady.value) return
  resetVisualEditing()
  previewVersion.value = Date.now()
}

const openPreview = () => {
  openExternalUrl(previewBaseUrl.value)
}

const handlePreviewLoad = (event: Event) => {
  const iframe = event.currentTarget as HTMLIFrameElement | null
  const doc = iframe?.contentDocument
  if (!doc) return

  doc.documentElement.style.minHeight = '100%'
  doc.documentElement.style.overflowY = 'auto'
  if (doc.body) {
    doc.body.style.minHeight = '100%'
    doc.body.style.overflowY = 'auto'
  }

  selectedElement.value = null
  const attached = iframe ? visualEditor.attach(iframe) : false
  if (isVisualEditing.value && (!attached || !visualEditor.setEnabled(true))) {
    resetVisualEditing()
    message.warning('预览页面必须与主站同源才能使用可视化编辑')
  }
}

const toggleVisualEditing = () => {
  if (isVisualEditing.value) {
    resetVisualEditing()
    return
  }

  const iframe = previewIframeRef.value
  if (!iframe || !visualEditor.attach(iframe) || !visualEditor.setEnabled(true)) {
    message.warning('预览页面必须与主站同源才能使用可视化编辑')
    return
  }

  isVisualEditing.value = true
  selectedElement.value = null
  mobilePanel.value = 'preview'
}

const handleDeploy = async () => {
  if (!isOwner.value || generating.value) return
  deploying.value = true
  try {
    const response = await deployApp({ appId: appId.value })
    if (response.data.code !== 0 || !response.data.data) {
      message.error(response.data.message || '部署失败')
      return
    }
    deployedUrl.value = response.data.data
    deployModalVisible.value = true
    if (appInfo.value)
      appInfo.value.deployKey = response.data.data.split('/').filter(Boolean).at(-1)
  } catch (error) {
    message.error(getApiErrorMessage(error, '部署失败，请先确认网站已经生成'))
  } finally {
    deploying.value = false
  }
}

const handleDownload = async () => {
  if (!isOwner.value || generating.value || downloading.value) return
  downloading.value = true
  try {
    // Long 类型应用 ID 超过 JavaScript 安全整数范围，必须保留路由中的原始字符串
    const response = await downloadAppCode(
      { appId: appId.value as unknown as number },
      { responseType: 'blob' },
    )
    const contentType = String(response.headers['content-type'] || response.data?.type || '')
    if (!contentType.includes('application/zip')) {
      const errorText = response.data instanceof Blob ? await response.data.text() : ''
      let errorMessage = '下载代码失败'
      try {
        errorMessage = JSON.parse(errorText).message || errorMessage
      } catch {
        errorMessage = errorText || errorMessage
      }
      throw new Error(errorMessage)
    }
    const contentDisposition = response.headers['content-disposition'] as string | undefined
    const encodedFileName = contentDisposition?.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
    const fileName = encodedFileName
      ? decodeURIComponent(encodedFileName)
      : contentDisposition?.match(/filename="?([^";]+)"?/i)?.[1] ||
        `${appInfo.value?.appName || '应用代码'}.zip`
    const downloadUrl = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    link.remove()
    setTimeout(() => URL.revokeObjectURL(downloadUrl), 0)
    message.success('代码下载已开始')
  } catch (error) {
    message.error(getApiErrorMessage(error, '下载代码失败，请先确认网站已经生成'))
  } finally {
    downloading.value = false
  }
}

const copyDeployUrl = async () => {
  try {
    await navigator.clipboard.writeText(deployedUrl.value)
    message.success('部署地址已复制')
  } catch {
    message.warning('复制失败，请手动复制地址')
  }
}

const openDeployedSite = () => {
  openExternalUrl(deployedUrl.value)
}

const loadApplication = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const response = await getAppVoById({ id: appId.value })
    if (response.data.code !== 0 || !response.data.data) {
      loadError.value = response.data.message || '应用不存在'
      return
    }

    appInfo.value = response.data.data
    if (!isOwner.value) {
      loadError.value = '你不是该应用的所有者，无法进入生成对话。'
      return
    }

    await loadChatHistory(true)

    if (messages.value.length === 0 && appInfo.value.initPrompt) {
      await startGeneration(appInfo.value.initPrompt)
    }
  } catch (error) {
    loadError.value = getApiErrorMessage(error, '应用加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadApplication)
onBeforeUnmount(() => {
  activeStream?.close()
  visualEditor.destroy()
})
</script>

<template>
  <main class="workspace">
    <header class="workspace__header">
      <button class="workspace-brand" type="button" @click="router.push('/')">
        <img src="/logo.png" alt="" />
        <span class="workspace-brand__text">
          <span class="workspace-brand__title">
            <strong>{{ appInfo?.appName || '应用生成工作台' }}</strong>
            <span v-if="appInfo?.codeGenType" class="workspace-brand__type">
              {{ formatCodeGenType(appInfo.codeGenType) }}
            </span>
          </span>
          <small>AI ZeroCode Workspace</small>
        </span>
      </button>
      <div class="workspace__header-actions">
        <a-button @click="router.push('/')">返回首页</a-button>
        <a-button :disabled="!previewReady" @click="refreshPreview">刷新预览</a-button>
        <a-button :loading="downloading" :disabled="!isOwner || generating" @click="handleDownload">
          下载代码
        </a-button>
        <a-button
          type="primary"
          :loading="deploying"
          :disabled="!isOwner || generating"
          @click="handleDeploy"
        >
          部署应用
        </a-button>
      </div>
    </header>

    <div class="mobile-switch" role="tablist" aria-label="工作区域切换">
      <button
        :class="{ active: mobilePanel === 'chat' }"
        type="button"
        @click="mobilePanel = 'chat'"
      >
        对话
      </button>
      <button
        :class="{ active: mobilePanel === 'preview' }"
        type="button"
        @click="mobilePanel = 'preview'"
      >
        预览
      </button>
    </div>

    <a-spin :spinning="loading" class="workspace__loader">
      <section v-if="loadError" class="workspace-error">
        <a-result status="403" title="无法进入对话" :sub-title="loadError">
          <template #extra>
            <a-button type="primary" @click="router.push('/')">返回首页</a-button>
          </template>
        </a-result>
      </section>

      <div v-else class="workspace__body">
        <section class="chat-panel" :class="{ 'mobile-hidden': mobilePanel !== 'chat' }">
          <div class="chat-panel__intro">
            <span class="status-dot" :class="{ active: generating }"></span>
            <div>
              <strong>{{ generating ? 'AI 正在构建网站' : '继续完善你的应用' }}</strong>
              <p>描述修改需求，AI 会流式输出内容；完成后右侧预览会自动刷新。</p>
            </div>
          </div>

          <div ref="messageListRef" class="message-list" @scroll="handleMessageScroll">
            <div v-if="hasMoreHistory || historyLoading" class="history-loader">
              <a-button type="link" :loading="historyLoading" @click="loadChatHistory(false)">
                {{ historyLoading ? '正在加载历史消息' : '加载更多历史消息' }}
              </a-button>
            </div>

            <article
              v-for="item in messages"
              :key="item.id"
              class="message-row"
              :class="`message-row--${item.role}`"
            >
              <div v-if="item.role === 'assistant'" class="message-avatar">
                <img src="/logo.png" alt="AI" />
              </div>
              <div
                class="message-bubble"
                :class="{
                  'message-bubble--error': item.status === 'error',
                  'message-bubble--streaming': item.status === 'streaming',
                }"
              >
                <MarkdownRenderer
                  v-if="item.role === 'assistant' && item.content"
                  :content="item.content"
                />
                <p v-else class="plain-message">{{ item.content || '正在思考并生成代码…' }}</p>
                <span
                  v-if="item.status === 'streaming' && item.content"
                  class="streaming-caret"
                  aria-hidden="true"
                ></span>
                <div v-if="item.status === 'streaming'" class="typing" aria-label="正在生成">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
                <button
                  v-if="item.status === 'error'"
                  class="retry-button"
                  type="button"
                  @click="retryMessage(item)"
                >
                  重试这条消息
                </button>
              </div>
            </article>
          </div>

          <button
            v-if="showScrollButton"
            class="scroll-bottom"
            type="button"
            @click="scrollToBottom(true)"
          >
            回到底部
          </button>

          <div class="composer">
            <a-alert
              v-if="selectedElement"
              class="selected-element-alert"
              type="info"
              show-icon
              closable
              @close="clearSelectedElement"
            >
              <template #message>已选择元素 {{ selectedElementLabel }}</template>
              <template #description>
                <div class="selected-element-info">
                  <code>{{ selectedElement.selector }}</code>
                  <span v-if="selectedElement.textContent">
                    {{ selectedElement.textContent }}
                  </span>
                </div>
              </template>
            </a-alert>
            <a-textarea
              v-model:value="inputMessage"
              :auto-size="{ minRows: 3, maxRows: 7 }"
              :disabled="generating"
              placeholder="描述希望修改或新增的内容，Enter 发送，Shift + Enter 换行"
              @keydown="handleInputKeydown"
            />
            <div class="composer__footer">
              <span>{{
                generating ? '生成期间暂不能发送新消息' : 'AI 可能会出错，请检查生成结果'
              }}</span>
              <div class="composer__actions">
                <a-button
                  class="visual-edit-button"
                  :class="{ 'visual-edit-button--active': isVisualEditing }"
                  :type="isVisualEditing ? 'primary' : 'default'"
                  :disabled="!previewReady || generating || !isOwner"
                  @click="toggleVisualEditing"
                >
                  {{ isVisualEditing ? '退出编辑' : '可视化编辑' }}
                </a-button>
                <a-button
                  type="primary"
                  :loading="generating"
                  :disabled="!inputMessage.trim()"
                  @click="sendCurrentMessage"
                >
                  发送
                </a-button>
              </div>
            </div>
          </div>
        </section>

        <section class="preview-panel" :class="{ 'mobile-hidden': mobilePanel !== 'preview' }">
          <div class="preview-toolbar">
            <div class="browser-dots" aria-hidden="true">
              <span></span>
              <span></span>
              <span></span>
            </div>
            <div class="preview-address">{{ previewBaseUrl || '等待生成网站' }}</div>
            <button type="button" :disabled="!previewReady" @click="openPreview">新窗口打开</button>
          </div>

          <div class="preview-stage">
            <iframe
              v-if="previewReady && previewUrl"
              :key="previewVersion"
              ref="previewIframeRef"
              :src="previewUrl"
              :title="`${appInfo?.appName || '应用'}网站预览`"
              scrolling="yes"
              sandbox="allow-scripts allow-forms allow-modals allow-popups allow-same-origin"
              @load="handlePreviewLoad"
            />
            <div v-else class="preview-empty">
              <div class="preview-empty__mark">
                <img src="/logo.png" alt="" />
              </div>
              <h2>{{ generating ? '正在生成你的网站' : '网站预览将在这里出现' }}</h2>
              <p>
                {{
                  generating
                    ? 'AI 正在理解需求、编写代码并保存文件，请稍候。'
                    : '发送第一条生成消息后，完成的网站会自动加载。'
                }}
              </p>
              <div v-if="generating" class="generation-steps">
                <span class="done">理解需求</span>
                <span class="active">生成代码</span>
                <span>加载预览</span>
              </div>
            </div>
            <div v-if="generating && previewReady" class="preview-updating">
              <span></span>
              正在更新网站，完成后自动刷新
            </div>
          </div>
        </section>
      </div>
    </a-spin>

    <a-modal v-model:open="deployModalVisible" title="应用部署成功" :footer="null" centered>
      <div class="deploy-result">
        <div class="deploy-result__icon">✓</div>
        <p>你的网站已经可以通过以下地址访问：</p>
        <a-input :value="deployedUrl" readonly />
        <div>
          <a-button @click="copyDeployUrl">复制地址</a-button>
          <a-button type="primary" @click="openDeployedSite">打开网站</a-button>
        </div>
      </div>
    </a-modal>
  </main>
</template>

<style scoped>
.workspace {
  position: fixed;
  inset: 0;
  display: flex;
  min-width: 320px;
  min-height: 0;
  overflow: hidden;
  flex-direction: column;
  background: #edf4f6;
}

.workspace__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 0 0 72px;
  gap: 20px;
  min-height: 0;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid var(--app-border);
}

.workspace-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 0;
  text-align: left;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.workspace-brand img {
  width: 46px;
  height: 46px;
  object-fit: cover;
  border-radius: 50%;
}

.workspace-brand__text {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.workspace-brand__title {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.workspace-brand strong {
  min-width: 0;
  max-width: 420px;
  overflow: hidden;
  color: var(--app-ink);
  font-size: 18px;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workspace-brand__type {
  flex: 0 0 auto;
  padding: 3px 8px;
  color: var(--app-blue-deep);
  font-size: 10px;
  font-weight: 700;
  line-height: 1.4;
  background: #eaf2ff;
  border-radius: 6px;
}

.workspace-brand small {
  color: var(--app-muted);
  font-size: 11px;
}

.workspace__header-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
}

.workspace__header-actions :deep(.ant-btn-primary) {
  background: var(--app-blue);
}

.workspace__loader {
  display: block;
  flex: 1 1 auto;
  min-height: 0;
}

.workspace :deep(.ant-spin-nested-loading) {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
}

.workspace :deep(.ant-spin-container) {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
}

.workspace__body {
  display: grid;
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: 10px;
  grid-template-columns: minmax(360px, 38%) minmax(0, 1fr);
  gap: 10px;
}

.chat-panel,
.preview-panel {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--app-border);
  border-radius: 18px;
  box-shadow: 0 8px 28px rgba(18, 62, 86, 0.06);
}

.chat-panel {
  position: relative;
  display: flex;
  flex-direction: column;
}

.chat-panel__intro {
  display: flex;
  align-items: flex-start;
  flex: 0 0 auto;
  gap: 11px;
  padding: 18px;
  background: linear-gradient(90deg, #f0fbf7, #f7fbff);
  border-bottom: 1px solid var(--app-border);
}

.chat-panel__intro strong {
  color: var(--app-ink);
  font-size: 15px;
}

.chat-panel__intro p {
  margin: 4px 0 0;
  color: var(--app-muted);
  font-size: 13px;
}

.status-dot {
  width: 10px;
  height: 10px;
  margin-top: 6px;
  background: #94a3b8;
  border-radius: 50%;
}

.status-dot.active {
  background: var(--app-primary);
  box-shadow: 0 0 0 5px rgba(16, 185, 129, 0.12);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  50% {
    box-shadow: 0 0 0 9px rgba(16, 185, 129, 0);
  }
}

.message-list {
  flex: 1 1 auto;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 22px 20px 30px;
  scroll-behavior: smooth;
}

.history-loader {
  display: flex;
  justify-content: center;
  min-height: 34px;
  margin-bottom: 16px;
  text-align: center;
}

.history-loader :deep(.ant-btn-link) {
  color: var(--app-blue-deep);
  font-size: 12px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  margin-bottom: 18px;
}

.message-row--user {
  justify-content: flex-end;
}

.message-avatar {
  flex: 0 0 30px;
  width: 30px;
  height: 30px;
  overflow: hidden;
  border-radius: 50%;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-bubble {
  max-width: min(86%, 720px);
  padding: 12px 14px;
  background: #f2f5f7;
  border-radius: 4px 16px 16px;
}

.message-row--user .message-bubble {
  color: #fff;
  background: linear-gradient(135deg, #0c9d83, #087c71);
  border-radius: 16px 4px 16px 16px;
}

.message-bubble--error {
  color: #9f2c2c;
  background: #fff0f0;
  border: 1px solid #ffd4d4;
}

.streaming-caret {
  display: inline-block;
  width: 7px;
  height: 1em;
  margin: 2px 0 0 3px;
  vertical-align: -0.12em;
  background: currentColor;
  border-radius: 1px;
  animation: caret-blink 900ms steps(2, start) infinite;
}

@keyframes caret-blink {
  50% {
    opacity: 0;
  }
}

.plain-message {
  margin: 0;
  overflow-wrap: anywhere;
  line-height: 1.72;
  white-space: pre-wrap;
}

.typing {
  display: flex;
  gap: 4px;
  margin-top: 9px;
}

.typing span {
  width: 5px;
  height: 5px;
  background: var(--app-primary);
  border-radius: 50%;
  animation: typing 1s infinite alternate;
}

.typing span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  to {
    opacity: 0.25;
    transform: translateY(-3px);
  }
}

.retry-button {
  margin-top: 8px;
  padding: 0;
  color: #b42318;
  font-size: 12px;
  font-weight: 700;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.scroll-bottom {
  position: absolute;
  right: 20px;
  bottom: 154px;
  z-index: 3;
  padding: 7px 12px;
  color: var(--app-blue-deep);
  font-size: 12px;
  background: #fff;
  border: 1px solid var(--app-border);
  border-radius: 999px;
  box-shadow: var(--app-shadow-soft);
  cursor: pointer;
}

.composer {
  flex: 0 0 auto;
  margin: 0 12px 12px;
  padding: 10px;
  background: #fff;
  border: 1px solid rgba(15, 42, 68, 0.14);
  border-radius: 15px;
  box-shadow: 0 -10px 24px rgba(18, 62, 86, 0.04);
}

.selected-element-alert {
  margin-bottom: 10px;
  text-align: left;
}

.selected-element-alert :deep(.ant-alert-message) {
  color: var(--app-ink);
  font-size: 13px;
  font-weight: 700;
}

.selected-element-info {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.selected-element-info code {
  overflow: hidden;
  color: var(--app-blue-deep);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-element-info span {
  display: -webkit-box;
  overflow: hidden;
  color: var(--app-muted);
  font-size: 12px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

:deep(.composer .ant-input) {
  max-height: 180px;
  padding: 4px 5px 10px;
  overflow-y: auto !important;
  border: 0;
  box-shadow: none;
  resize: none;
}

.composer__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 7px 2px 0;
  border-top: 1px solid var(--app-border);
}

.composer__footer span {
  color: #8b97a7;
  font-size: 11px;
}

.composer__actions {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 8px;
}

.composer__actions :deep(.visual-edit-button--active) {
  background: var(--app-blue);
  border-color: var(--app-blue);
}

.composer__footer :deep(.ant-btn-primary) {
  background: var(--app-primary);
}

.composer__footer :deep(.visual-edit-button--active) {
  background: var(--app-blue);
}

.preview-panel {
  display: flex;
  flex-direction: column;
  background: #e8eef2;
}

.preview-toolbar {
  display: flex;
  align-items: center;
  flex: 0 0 48px;
  gap: 14px;
  min-height: 0;
  padding: 8px 13px;
  background: #fff;
  border-bottom: 1px solid var(--app-border);
}

.browser-dots {
  display: flex;
  gap: 5px;
}

.browser-dots span {
  width: 9px;
  height: 9px;
  background: #ff6b64;
  border-radius: 50%;
}

.browser-dots span:nth-child(2) {
  background: #f7bb3e;
}

.browser-dots span:nth-child(3) {
  background: #34c759;
}

.preview-address {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  padding: 5px 12px;
  color: #7c8998;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #f3f6f8;
  border-radius: 7px;
}

.preview-toolbar button {
  flex: 0 0 auto;
  color: var(--app-blue);
  font-size: 12px;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.preview-toolbar button:disabled {
  color: #abb4bf;
  cursor: not-allowed;
}

.preview-stage {
  position: relative;
  flex: 1 1 auto;
  min-height: 0;
  margin: 12px;
  overflow: hidden;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 12px 28px rgba(18, 62, 86, 0.12);
}

.preview-stage iframe {
  display: block;
  width: 100%;
  height: 100%;
  overflow: auto;
  border: 0;
}

.preview-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 30px;
  flex-direction: column;
  text-align: center;
  background: radial-gradient(circle at 50% 35%, #e8fbf5, #fff 48%);
}

.preview-empty__mark {
  width: 78px;
  height: 78px;
  padding: 5px;
  background: #fff;
  border: 1px solid var(--app-border);
  border-radius: 50%;
  box-shadow: var(--app-shadow-soft);
}

.preview-empty__mark img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.preview-empty h2 {
  margin: 22px 0 7px;
  color: var(--app-ink);
}

.preview-empty p {
  max-width: 440px;
  margin: 0;
  color: var(--app-muted);
}

.generation-steps {
  display: flex;
  gap: 7px;
  margin-top: 26px;
}

.generation-steps span {
  padding: 6px 10px;
  color: #8b97a7;
  font-size: 11px;
  background: #f0f3f5;
  border-radius: 999px;
}

.generation-steps .done {
  color: #087c71;
  background: #e5f8f2;
}

.generation-steps .active {
  color: #fff;
  background: var(--app-primary);
}

.preview-updating {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #fff;
  font-weight: 700;
  background: rgba(8, 28, 46, 0.55);
  backdrop-filter: blur(3px);
}

.preview-updating span {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.workspace-error {
  display: grid;
  width: 100%;
  height: 100%;
  margin: 10px;
  place-items: center;
  background: #fff;
  border-radius: 18px;
}

.mobile-switch {
  display: none;
}

.deploy-result {
  padding: 6px 0 12px;
  text-align: center;
}

.deploy-result__icon {
  display: grid;
  width: 54px;
  height: 54px;
  margin: 0 auto 18px;
  place-items: center;
  color: #fff;
  font-size: 28px;
  background: var(--app-primary);
  border-radius: 50%;
}

.deploy-result p {
  color: var(--app-muted);
}

.deploy-result > div:last-child {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 18px;
}

@media (max-width: 760px) {
  .workspace__header {
    flex: 0 0 62px;
    padding: 8px 10px;
  }

  .workspace-brand img {
    width: 42px;
    height: 42px;
  }

  .workspace-brand small {
    display: none;
  }

  .workspace-brand strong {
    max-width: 132px;
    font-size: 15px;
  }

  .workspace__header-actions > :deep(.ant-btn:first-child),
  .workspace__header-actions > :deep(.ant-btn:nth-child(2)) {
    display: none;
  }

  .mobile-switch {
    display: grid;
    grid-template-columns: 1fr 1fr;
    flex: 0 0 44px;
    gap: 4px;
    min-height: 0;
    padding: 5px 10px;
    background: #fff;
    border-bottom: 1px solid var(--app-border);
  }

  .mobile-switch button {
    color: var(--app-muted);
    background: transparent;
    border: 0;
    border-radius: 8px;
  }

  .mobile-switch button.active {
    color: var(--app-primary-deep);
    font-weight: 700;
    background: #eaf9f4;
  }

  .workspace__body {
    display: block;
    padding: 6px;
  }

  .chat-panel,
  .preview-panel {
    width: 100%;
    height: 100%;
    border-radius: 13px;
  }

  .mobile-hidden {
    display: none;
  }

  .message-list {
    padding-inline: 12px;
  }

  .message-bubble {
    max-width: 90%;
  }

  .composer__footer > span {
    display: none;
  }

  .composer__actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
