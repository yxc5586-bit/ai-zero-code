<script setup lang="ts">
defineProps<{ status: '403' | '404' }>()
</script>

<template>
  <main class="status-view page-enter">
    <a-result
      :status="status"
      :title="status"
      :sub-title="status === '403' ? '你没有访问这个页面的权限' : '你访问的页面不存在或已被移除'"
    >
      <template #extra>
        <RouterLink to="/"><a-button type="primary">返回首页</a-button></RouterLink>
      </template>
    </a-result>
  </main>
</template>

<style scoped>
.status-view {
  width: min(100% - 32px, 960px);
  margin: 70px auto;
  padding: 36px;
  background: var(--app-surface);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
}
</style>
