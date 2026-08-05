<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { useLoginUserStore } from '@/stores/loginUser'
import { formatRelativeTime, getDeployUrl } from '@/utils/format'

const props = defineProps<{ app: API.AppVO; editable?: boolean }>()
const emit = defineEmits<{ delete: [app: API.AppVO] }>()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const isOwner = computed(
  () => Boolean(props.app.userId && props.app.userId === loginUserStore.loginUser?.id),
)
const deployUrl = computed(() => getDeployUrl(props.app.deployKey))
const creatorName = computed(
  () => props.app.createUser?.userName || props.app.createUser?.userAccount || 'AI ZeroCode 用户',
)

const enterChat = () => {
  if (isOwner.value && props.app.id) router.push({ name: 'appChat', params: { id: props.app.id } })
}
const openDeploy = () => {
  if (deployUrl.value) window.open(deployUrl.value, '_blank', 'noopener,noreferrer')
}
const editApp = () => {
  if (props.app.id) router.push({ name: 'appEdit', params: { id: props.app.id }, query: { from: 'home' } })
}
</script>

<template>
  <article class="app-card" tabindex="0">
    <div class="app-card__media">
      <img v-if="app.cover" :src="app.cover" :alt="`${app.appName || '应用'}封面`" loading="lazy" />
      <div v-else class="app-card__placeholder">
        <div class="placeholder-mark"><img src="/logo.png" alt="" /></div>
        <strong>{{ app.appName || '未命名应用' }}</strong>
        <span>{{ app.codeGenType === 'html' ? 'HTML 应用' : '多文件网站应用' }}</span>
      </div>

      <div class="app-card__badges">
        <span v-if="app.priority === 99" class="badge badge--featured">精选</span>
        <span v-if="app.deployKey" class="badge badge--deployed">已部署</span>
      </div>

      <div class="app-card__actions">
        <a-button v-if="isOwner" class="action-button" @click="enterChat">进入对话</a-button>
        <a-tooltip :title="deployUrl ? '打开部署网站' : '暂未部署'">
          <a-button class="action-button action-button--primary" :disabled="!deployUrl" @click="openDeploy">
            打开部署
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

    <div class="app-card__body">
      <div class="app-card__title-row">
        <h3>{{ app.appName || '未命名应用' }}</h3>
        <span>{{ app.codeGenType === 'html' ? 'HTML' : '多文件' }}</span>
      </div>
      <p>{{ app.initPrompt || '这个应用还没有补充描述。' }}</p>
      <div class="app-card__meta">
        <a-avatar :size="28" :src="app.createUser?.userAvatar">{{ creatorName.slice(0, 1) }}</a-avatar>
        <span>{{ creatorName }}</span>
        <i aria-hidden="true"></i>
        <time>{{ formatRelativeTime(app.createTime) }}</time>
      </div>
    </div>
  </article>
</template>

<style scoped>
.app-card { position: relative; min-width: 0; overflow: hidden; background: rgba(255,255,255,.88); border: 1px solid rgba(255,255,255,.92); border-radius: var(--app-radius-md); box-shadow: 0 10px 30px rgba(24,74,96,.07); transition: transform var(--app-transition), box-shadow var(--app-transition), border-color var(--app-transition); }
.app-card:hover, .app-card:focus-within { transform: translateY(-4px); border-color: rgba(16,185,129,.22); box-shadow: 0 22px 46px rgba(24,74,96,.13); }
.app-card__media { position: relative; aspect-ratio: 16 / 9; overflow: hidden; background: #e8f4f5; }
.app-card__media > img { width: 100%; height: 100%; object-fit: cover; transition: transform 420ms ease; }
.app-card:hover .app-card__media > img { transform: scale(1.025); }
.app-card__placeholder { position: absolute; inset: 0; display: flex; flex-direction: column; justify-content: flex-end; padding: 26px; color: #fff; background: linear-gradient(145deg, #0e8777, #1665c1); }
.app-card__placeholder::before { position: absolute; inset: 0; content: ''; opacity: .35; background: radial-gradient(circle at 75% 20%, rgba(255,255,255,.45), transparent 23%), linear-gradient(115deg, transparent 62%, rgba(255,255,255,.13) 62%); }
.placeholder-mark { position: absolute; top: 22px; right: 22px; width: 56px; height: 56px; overflow: hidden; border: 3px solid rgba(255,255,255,.66); border-radius: 50%; }
.placeholder-mark img { width: 100%; height: 100%; object-fit: cover; }
.app-card__placeholder strong, .app-card__placeholder span { position: relative; z-index: 1; }
.app-card__placeholder strong { font-size: 22px; }
.app-card__placeholder span { margin-top: 4px; color: rgba(255,255,255,.72); font-size: 13px; }
.app-card__badges { position: absolute; top: 14px; left: 14px; z-index: 3; display: flex; gap: 6px; }
.badge { padding: 5px 9px; color: #fff; font-size: 11px; font-weight: 700; border-radius: 999px; backdrop-filter: blur(10px); }
.badge--featured { background: rgba(124,58,237,.86); }
.badge--deployed { background: rgba(5,150,105,.86); }
.more-button { position: absolute; top: 12px; right: 12px; z-index: 5; width: 36px; height: 32px; color: #fff; font-weight: 800; letter-spacing: 2px; background: rgba(13,27,42,.58); border: 1px solid rgba(255,255,255,.35); border-radius: 999px; cursor: pointer; backdrop-filter: blur(8px); }
.app-card__actions { position: absolute; inset: 0; z-index: 2; display: flex; align-items: center; justify-content: center; gap: 10px; padding: 20px; opacity: 0; background: rgba(8,28,46,.62); backdrop-filter: blur(3px); transition: opacity var(--app-transition); }
.app-card:hover .app-card__actions, .app-card:focus-within .app-card__actions { opacity: 1; }
.action-button { color: var(--app-text); background: #fff; border-color: #fff; }
.action-button--primary { color: #fff; background: var(--app-blue); border-color: var(--app-blue); }
.app-card__body { padding: 18px 19px 20px; }
.app-card__title-row { display: flex; align-items: center; gap: 10px; }
.app-card__title-row h3 { min-width: 0; margin: 0; overflow: hidden; color: var(--app-ink); font-size: 18px; text-overflow: ellipsis; white-space: nowrap; }
.app-card__title-row > span { flex-shrink: 0; padding: 3px 7px; color: var(--app-blue-deep); font-size: 10px; font-weight: 700; background: #eaf2ff; border-radius: 6px; }
.app-card__body > p { height: 42px; margin: 10px 0 16px; overflow: hidden; color: var(--app-muted); font-size: 13px; line-height: 21px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.app-card__meta { display: flex; align-items: center; gap: 8px; color: var(--app-muted); font-size: 12px; }
.app-card__meta i { width: 3px; height: 3px; margin-left: auto; background: #b5c0cf; border-radius: 50%; }
.app-card__meta time { white-space: nowrap; }
@media (hover: none) {
  .app-card__actions { inset: auto 10px 10px auto; padding: 0; opacity: 1; background: transparent; backdrop-filter: none; }
  .action-button { height: 30px; padding-inline: 10px; font-size: 12px; box-shadow: 0 6px 18px rgba(8,28,46,.2); }
  .app-card__body { padding-bottom: 54px; }
}
</style>
