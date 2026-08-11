<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import AppCover from '@/components/AppCover.vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  formatCodeGenType,
  formatRelativeTime,
  getDeployUrl,
  openExternalUrl,
} from '@/utils/format'

const props = defineProps<{ app: API.AppVO; editable?: boolean }>()
const emit = defineEmits<{
  delete: [app: API.AppVO]
  detail: [app: API.AppVO]
}>()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const isOwner = computed(() =>
  Boolean(props.app.userId && props.app.userId === loginUserStore.loginUser?.id),
)
const deployUrl = computed(() => getDeployUrl(props.app.deployKey))
const creatorName = computed(
  () => props.app.createUser?.userName || props.app.createUser?.userAccount || 'AI ZeroCode 用户',
)

const enterChat = () => {
  if (isOwner.value && props.app.id) router.push({ name: 'appChat', params: { id: props.app.id } })
}
const openDeploy = () => {
  openExternalUrl(deployUrl.value)
}
const editApp = () => {
  if (props.app.id)
    router.push({ name: 'appEdit', params: { id: props.app.id }, query: { from: 'home' } })
}
</script>

<template>
  <article class="app-card">
    <div class="app-card__media">
      <AppCover :app="app" />

      <div class="app-card__badges">
        <span v-if="app.priority === 99" class="badge badge--featured">精选</span>
        <span v-if="app.deployKey" class="badge badge--deployed">已部署</span>
      </div>

      <div class="app-card__actions">
        <a-button class="action-button" @click="emit('detail', app)"> 查看详情 </a-button>
        <a-button v-if="isOwner" class="action-button" @click="enterChat"> 继续创作 </a-button>
        <a-tooltip :title="deployUrl ? '打开部署网站' : '暂未部署'">
          <a-button
            class="action-button action-button--primary"
            :disabled="!deployUrl"
            @click="openDeploy"
          >
            预览网站
          </a-button>
        </a-tooltip>
      </div>

      <a-dropdown v-if="editable && isOwner" placement="bottomRight" :trigger="['click']">
        <button class="more-button" type="button" aria-label="更多应用操作" @click.stop>•••</button>
        <template #overlay>
          <a-menu>
            <a-menu-item key="edit" @click="editApp">编辑信息</a-menu-item>
            <a-menu-divider />
            <a-menu-item key="delete" danger @click="emit('delete', app)">删除应用</a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </div>

    <div
      class="app-card__body"
      role="button"
      tabindex="0"
      @click="emit('detail', app)"
      @keydown.enter="emit('detail', app)"
      @keydown.space.prevent="emit('detail', app)"
    >
      <div class="app-card__title-row">
        <h3>{{ app.appName || '未命名应用' }}</h3>
        <span>{{ formatCodeGenType(app.codeGenType) }}</span>
      </div>
      <p>{{ app.initPrompt || '这个应用还没有补充描述。' }}</p>
      <div class="app-card__meta">
        <a-avatar :size="24" :src="app.createUser?.userAvatar">{{
          creatorName.slice(0, 1)
        }}</a-avatar>
        <span>{{ creatorName }}</span>
        <i aria-hidden="true"></i>
        <time>{{ formatRelativeTime(app.createTime) }}</time>
      </div>
    </div>
  </article>
</template>

<style scoped>
.app-card {
  position: relative;
  min-width: 0;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(255, 255, 255, 0.92);
  border-radius: 13px;
  box-shadow: 0 7px 20px rgba(24, 74, 96, 0.07);
  transition:
    transform var(--app-transition),
    box-shadow var(--app-transition),
    border-color var(--app-transition);
}
.app-card:hover,
.app-card:focus-within {
  transform: translateY(-2px);
  border-color: rgba(16, 185, 129, 0.22);
  box-shadow: 0 14px 30px rgba(24, 74, 96, 0.12);
}
.app-card__media {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: #e8f4f5;
}
.app-card:hover :deep(.app-cover > img) {
  transform: scale(1.025);
}
.app-card__badges {
  position: absolute;
  top: 9px;
  left: 9px;
  z-index: 3;
  display: flex;
  gap: 5px;
}
.badge {
  padding: 3px 7px;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  border-radius: 999px;
  backdrop-filter: blur(10px);
}
.badge--featured {
  background: rgba(124, 58, 237, 0.86);
}
.badge--deployed {
  background: rgba(5, 150, 105, 0.86);
}
.more-button {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 5;
  width: 31px;
  height: 27px;
  color: #fff;
  font-weight: 800;
  letter-spacing: 2px;
  background: rgba(13, 27, 42, 0.58);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 999px;
  cursor: pointer;
  backdrop-filter: blur(8px);
}
.app-card__actions {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  align-items: flex-end;
  gap: 7px;
  padding: 36px 9px 9px;
  opacity: 0;
  pointer-events: none;
  background: linear-gradient(180deg, rgba(5, 24, 37, 0.04) 16%, rgba(5, 32, 43, 0.68) 100%);
  transition:
    opacity var(--app-transition),
    background var(--app-transition);
}
.app-card:hover .app-card__actions,
.app-card:focus-within .app-card__actions {
  opacity: 1;
  pointer-events: auto;
}
.action-button {
  min-width: 0;
  height: 39px;
  flex: 1 1 0;
  padding-inline: 9px;
  color: var(--app-text);
  font-size: 12px;
  font-weight: 650;
  background: rgba(255, 255, 255, 0.96);
  border-color: rgba(255, 255, 255, 0.82);
  border-radius: 10px;
  box-shadow: 0 8px 20px rgba(4, 25, 37, 0.16);
  transform: translateY(8px);
  transition:
    color var(--app-transition),
    background var(--app-transition),
    border-color var(--app-transition),
    box-shadow var(--app-transition),
    transform var(--app-transition);
}
.app-card:hover .action-button,
.app-card:focus-within .action-button {
  transform: translateY(0);
}
.action-button:hover,
.action-button:focus-visible {
  color: var(--app-primary-deep);
  background: #fff;
  border-color: rgba(16, 185, 129, 0.42);
  box-shadow: 0 10px 24px rgba(4, 25, 37, 0.22);
}
.action-button--primary {
  color: #fff;
  background: var(--app-primary);
  border-color: var(--app-primary);
}
.action-button--primary:hover,
.action-button--primary:focus-visible {
  color: #fff;
  background: var(--app-primary-deep);
  border-color: var(--app-primary-deep);
}
.action-button--primary:disabled {
  color: rgba(255, 255, 255, 0.8);
  background: rgba(104, 125, 132, 0.78);
  border-color: transparent;
}
.app-card__body {
  display: block;
  width: 100%;
  padding: 12px 13px 14px;
  text-align: left;
  background: transparent;
  border: 0;
  cursor: pointer;
}
.app-card__title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.app-card__title-row h3 {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: var(--app-ink);
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.app-card__title-row > span {
  flex-shrink: 0;
  padding: 3px 7px;
  color: var(--app-blue-deep);
  font-size: 10px;
  font-weight: 700;
  background: #eaf2ff;
  border-radius: 6px;
}
.app-card__body > p {
  height: 36px;
  margin: 7px 0 11px;
  overflow: hidden;
  color: var(--app-muted);
  font-size: 12px;
  line-height: 18px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.app-card__meta {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--app-muted);
  font-size: 11px;
}
.app-card__meta i {
  width: 3px;
  height: 3px;
  margin-left: auto;
  background: #b5c0cf;
  border-radius: 50%;
}
.app-card__meta time {
  white-space: nowrap;
}
@media (hover: none) {
  .app-card__actions {
    padding: 40px 8px 8px;
    opacity: 1;
    pointer-events: auto;
  }
  .action-button {
    height: 37px;
    transform: none;
  }
}
@media (prefers-reduced-motion: reduce) {
  .app-card,
  .action-button,
  .app-card__actions,
  .app-card :deep(.app-cover > img) {
    transition: none;
  }
}
</style>
