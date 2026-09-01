<script setup lang="ts">
import { computed } from 'vue'

import AppCover from '@/components/AppCover.vue'
import {
  formatCodeGenType,
  formatDateTime,
  getDeployUrl,
  getStaticPreviewUrl,
  openExternalUrl,
} from '@/utils/format'

const props = withDefaults(
  defineProps<{
    open: boolean
    app: API.AppVO | null
    editable?: boolean
  }>(),
  { editable: false },
)

const emit = defineEmits<{
  'update:open': [open: boolean]
  edit: [app: API.AppVO]
}>()

const previewUrl = computed(() =>
  props.app?.artifactAvailable ? getStaticPreviewUrl(props.app) : '',
)
const deployUrl = computed(() =>
  props.app?.deploymentAvailable
    ? getDeployUrl(props.app.deployKey, props.app.deployUrl)
    : '',
)
const creatorName = computed(
  () =>
    props.app?.createUser?.userName ||
    props.app?.createUser?.userAccount ||
    props.app?.userId ||
    '—',
)
</script>

<template>
  <a-drawer
    :open="open"
    title="应用详情"
    width="min(540px, 94vw)"
    @update:open="emit('update:open', $event)"
  >
    <template v-if="app">
      <div class="detail-cover">
        <AppCover :app="app" />
      </div>
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="应用名称">{{ app.appName || '—' }}</a-descriptions-item>
        <a-descriptions-item label="应用 ID">{{ app.id || '—' }}</a-descriptions-item>
        <a-descriptions-item label="创建用户">{{ creatorName }}</a-descriptions-item>
        <a-descriptions-item label="生成类型">{{
          formatCodeGenType(app.codeGenType)
        }}</a-descriptions-item>
        <a-descriptions-item label="优先级">{{ app.priority ?? 0 }}</a-descriptions-item>
        <a-descriptions-item label="部署状态">
          <a-badge
            :status="app.deploymentAvailable ? 'success' : app.deployKey ? 'warning' : 'default'"
            :text="app.deploymentAvailable ? '已部署' : app.deployKey ? '部署产物已过期' : '未部署'"
          />
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">
          {{ formatDateTime(app.createTime) }}
        </a-descriptions-item>
        <a-descriptions-item label="部署时间">
          {{ formatDateTime(app.deployedTime) }}
        </a-descriptions-item>
        <a-descriptions-item label="初始提示词">
          <p class="detail-prompt">{{ app.initPrompt || '—' }}</p>
        </a-descriptions-item>
      </a-descriptions>
      <div class="drawer-actions">
        <a-button :disabled="!previewUrl" @click="openExternalUrl(previewUrl)">静态预览</a-button>
        <a-button :disabled="!deployUrl" @click="openExternalUrl(deployUrl)">部署网站</a-button>
        <a-button v-if="editable" type="primary" @click="emit('edit', app)">编辑应用</a-button>
      </div>
    </template>
  </a-drawer>
</template>

<style scoped>
.detail-cover {
  aspect-ratio: 16 / 8;
  margin-bottom: 18px;
  overflow: hidden;
  border-radius: 12px;
}

.detail-prompt {
  margin: 0;
  line-height: 1.7;
  white-space: pre-wrap;
}

.drawer-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 18px;
}
</style>
