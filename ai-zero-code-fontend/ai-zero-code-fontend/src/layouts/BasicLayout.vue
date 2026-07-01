<script setup lang="ts">
import { RouterView } from 'vue-router'

import GlobalFooter from '@/components/GlobalFooter.vue'
import GlobalHeader from '@/components/GlobalHeader.vue'
</script>

<template>
  <a-layout class="basic-layout">
    <a-layout-header class="basic-layout__header">
      <GlobalHeader />
    </a-layout-header>
    <a-layout-content class="basic-layout__content">
      <div class="basic-layout__content-inner">
        <RouterView />
      </div>
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
  padding-inline: 24px;
  background: transparent;
  line-height: normal;
}

.basic-layout__content {
  flex: 1;
  padding: 28px 24px calc(var(--app-footer-height) + 36px);
  background: transparent;
}

.basic-layout__content-inner {
  width: min(100%, var(--app-content-width));
  margin: 0 auto;
}

.basic-layout__footer {
  position: fixed;
  inset-inline: 0;
  bottom: 0;
  z-index: 90;
  height: var(--app-footer-height);
  padding: 0 24px;
  background: transparent;
}

@media (max-width: 767px) {
  .basic-layout__header,
  .basic-layout__content,
  .basic-layout__footer {
    padding-inline: 16px;
  }

  .basic-layout__content {
    padding-top: 20px;
    padding-bottom: calc(var(--app-footer-height) + 24px);
  }
}
</style>
