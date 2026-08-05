<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import css from 'highlight.js/lib/languages/css'
import javascript from 'highlight.js/lib/languages/javascript'
import xml from 'highlight.js/lib/languages/xml'
import 'highlight.js/styles/github-dark.css'

const props = defineProps<{ content: string }>()

hljs.registerLanguage('xml', xml)
hljs.registerLanguage('css', css)
hljs.registerLanguage('javascript', javascript)

const languageMap: Record<string, string> = {
  html: 'xml',
  xml: 'xml',
  css: 'css',
  js: 'javascript',
  javascript: 'javascript',
}

const escapeAttribute = (value: string) =>
  value.replace(/[&<>"']/g, (character) => {
    const entities: Record<string, string> = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;',
    }
    return entities[character] || character
  })

const markdown = new MarkdownIt({
  // AI 返回内容不应执行任意 HTML；HTML 代码请放在代码围栏中展示。
  html: false,
  breaks: true,
  linkify: true,
  typographer: true,
  highlight(code, language): string {
    const requestedLanguage = language.trim().split(/\s+/)[0]?.toLowerCase() || ''
    const normalizedLanguage = languageMap[requestedLanguage]
    if (!normalizedLanguage) return ''

    const highlighted = hljs.highlight(code, { language: normalizedLanguage }).value
    const label = escapeAttribute(requestedLanguage.toUpperCase())
    return `<pre class="hljs" data-language="${label}"><code>${highlighted}</code></pre>`
  },
})

const renderedContent = computed(() => markdown.render(props.content || ''))
</script>

<template>
  <div class="markdown-body" v-html="renderedContent"></div>
</template>

<style scoped>
.markdown-body {
  min-width: 0;
  overflow-wrap: anywhere;
  color: inherit;
  font-size: 13px;
  line-height: 1.72;
}

.markdown-body :deep(> :first-child) {
  margin-top: 0;
}

.markdown-body :deep(> :last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(p),
.markdown-body :deep(ul),
.markdown-body :deep(ol),
.markdown-body :deep(blockquote),
.markdown-body :deep(pre),
.markdown-body :deep(table) {
  margin: 0 0 12px;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 18px 0 8px;
  color: var(--app-ink);
  font-weight: 750;
  line-height: 1.35;
}

.markdown-body :deep(h1) {
  font-size: 20px;
}

.markdown-body :deep(h2) {
  padding-bottom: 5px;
  font-size: 17px;
  border-bottom: 1px solid var(--app-border);
}

.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  font-size: 15px;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 22px;
}

.markdown-body :deep(li + li) {
  margin-top: 4px;
}

.markdown-body :deep(a) {
  color: var(--app-blue);
  text-decoration: underline;
  text-decoration-color: rgba(23, 105, 224, 0.3);
  text-underline-offset: 3px;
}

.markdown-body :deep(blockquote) {
  padding: 8px 12px;
  color: #536174;
  background: #eef6f5;
  border-left: 3px solid var(--app-primary);
  border-radius: 0 8px 8px 0;
}

.markdown-body :deep(code:not(pre code)) {
  padding: 2px 5px;
  color: #b4235a;
  font-family: 'Cascadia Code', 'SFMono-Regular', Consolas, monospace;
  font-size: 0.9em;
  background: #f6eefa;
  border-radius: 5px;
}

.markdown-body :deep(pre) {
  position: relative;
  max-width: 100%;
  overflow: auto;
  padding: 30px 14px 14px;
  color: #e6edf3;
  font-family: 'Cascadia Code', 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  line-height: 1.65;
  background: #0d1117;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
}

.markdown-body :deep(pre[data-language]::before) {
  position: absolute;
  top: 8px;
  left: 13px;
  color: #8b949e;
  font-family: inherit;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  content: attr(data-language);
}

.markdown-body :deep(pre code) {
  padding: 0;
  color: inherit;
  background: transparent;
}

.markdown-body :deep(table) {
  display: block;
  max-width: 100%;
  overflow-x: auto;
  border-spacing: 0;
  border-collapse: collapse;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 7px 10px;
  text-align: left;
  border: 1px solid var(--app-border);
}

.markdown-body :deep(th) {
  color: var(--app-ink);
  background: #f3f7f8;
}
</style>
