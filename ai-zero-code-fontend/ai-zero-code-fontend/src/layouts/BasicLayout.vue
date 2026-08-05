<script setup lang="ts">
import GlobalFooter from '@/components/GlobalFooter.vue'
import GlobalHeader from '@/components/GlobalHeader.vue'
</script>

<template>
  <a-layout class="basic-layout">
    <a-layout-header class="basic-layout__header">
      <GlobalHeader />
    </a-layout-header>
    <a-layout-content class="basic-layout__content">
      <RouterView />
    </a-layout-content>
    <a-layout-footer class="basic-layout__footer">
      <GlobalFooter />
    </a-layout-footer>
  </a-layout>
</template>

<style scoped>
.basic-layout {
  min-height: 100vh;
  background: transparent;
}

.basic-layout__header {
  position: sticky;
  top: 0;
  z-index: 100;
  height: var(--app-header-height);
  padding: 10px 24px;
  line-height: normal;
  background: rgba(246, 253, 251, 0.76);
  border-bottom: 1px solid rgba(15, 42, 68, 0.06);
  backdrop-filter: blur(20px);
}
.basic-layout__content { min-height: calc(100vh - 148px); background: transparent; }
.basic-layout__footer { padding: 0 24px 24px; background: transparent; }

@media (max-width: 767px) {
  .basic-layout__header { padding-inline: 14px; }
  .basic-layout__footer { padding: 0 14px 18px; }
}
</style>
