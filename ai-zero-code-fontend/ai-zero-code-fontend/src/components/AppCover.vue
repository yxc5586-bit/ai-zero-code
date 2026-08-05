<script setup lang="ts">
withDefaults(
  defineProps<{
    app: Pick<API.AppVO, 'appName' | 'cover' | 'codeGenType'>
    compact?: boolean
  }>(),
  { compact: false },
)
</script>

<template>
  <div class="app-cover" :class="{ 'app-cover--compact': compact }">
    <img v-if="app.cover" :src="app.cover" :alt="`${app.appName || '应用'}封面`" loading="lazy" />
    <div v-else class="app-cover__placeholder">
      <img src="/logo.png" alt="" />
      <template v-if="!compact">
        <strong>{{ app.appName || '未命名应用' }}</strong>
        <span>{{ app.codeGenType === 'html' ? 'HTML 应用' : '多文件网站应用' }}</span>
      </template>
    </div>
  </div>
</template>

<style scoped>
.app-cover {
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #e8f4f5;
}

.app-cover > img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 360ms ease;
}

.app-cover__placeholder {
  position: relative;
  display: flex;
  justify-content: flex-end;
  width: 100%;
  height: 100%;
  padding: 18px;
  overflow: hidden;
  color: #fff;
  flex-direction: column;
  background: linear-gradient(145deg, #0e8777, #1665c1);
}

.app-cover__placeholder::before {
  position: absolute;
  inset: 0;
  content: '';
  opacity: 0.35;
  background:
    radial-gradient(circle at 75% 20%, rgba(255, 255, 255, 0.45), transparent 23%),
    linear-gradient(115deg, transparent 62%, rgba(255, 255, 255, 0.13) 62%);
}

.app-cover__placeholder img {
  position: absolute;
  top: 15px;
  right: 15px;
  width: 42px;
  height: 42px;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.66);
  border-radius: 50%;
}

.app-cover__placeholder strong,
.app-cover__placeholder span {
  position: relative;
  z-index: 1;
}

.app-cover__placeholder strong {
  font-size: 17px;
}

.app-cover__placeholder span {
  margin-top: 2px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 11px;
}

.app-cover--compact .app-cover__placeholder {
  align-items: center;
  justify-content: center;
  padding: 0;
}

.app-cover--compact .app-cover__placeholder img {
  position: relative;
  inset: auto;
  width: 60%;
  height: 60%;
  border-width: 1px;
}
</style>
