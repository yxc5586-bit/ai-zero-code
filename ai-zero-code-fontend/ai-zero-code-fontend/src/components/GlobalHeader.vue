<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

type MenuItem = {
  key: string
  label: string
  path: string
}

const menuItems: MenuItem[] = [
  { key: 'home', label: '首页', path: '/' },
  { key: 'about', label: '关于我们', path: '/about' },
]

const route = useRoute()
const router = useRouter()

const selectedKeys = computed(() => {
  const currentMenu = menuItems.find((item) => route.path.startsWith(item.path) && item.path !== '/')

  if (currentMenu) {
    return [currentMenu.key]
  }

  return route.path === '/' ? ['home'] : []
})

const handleMenuClick = ({ key }: { key: string }) => {
  const target = menuItems.find((item) => item.key === key)

  if (target && target.path !== route.path) {
    router.push(target.path)
  }
}
</script>

<template>
  <div class="global-header">
    <RouterLink class="global-header__brand" to="/">
      <img alt="logo" class="global-header__logo" src="/logo.png" />
      <div class="global-header__brand-copy">
        <span class="global-header__title">AI 零代码平台</span>
        <span class="global-header__subtitle">Build faster with configurable workflows</span>
      </div>
    </RouterLink>

    <a-menu
      mode="horizontal"
      class="global-header__menu"
      :selected-keys="selectedKeys"
      @click="handleMenuClick"
    >
      <a-menu-item v-for="item in menuItems" :key="item.key">
        {{ item.label }}
      </a-menu-item>
    </a-menu>

    <div class="global-header__actions">
      <a-button type="primary" size="large">登录</a-button>
    </div>
  </div>
</template>

<style scoped>
.global-header {
  display: flex;
  align-items: center;
  gap: 20px;
  width: min(100%, var(--app-content-width));
  height: 100%;
  margin: 0 auto;
  padding: 10px 18px;
  background: var(--app-surface);
  border: 1px solid var(--app-border);
  border-radius: calc(var(--app-radius-large) - 6px);
  box-shadow: var(--app-shadow);
  backdrop-filter: blur(18px);
}

.global-header__brand {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
  min-width: fit-content;
}

.global-header__logo {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  box-shadow: 0 10px 20px rgba(22, 119, 255, 0.18);
}

.global-header__brand-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.global-header__title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--app-text);
  letter-spacing: 0.01em;
}

.global-header__subtitle {
  color: var(--app-text-secondary);
  font-size: 0.75rem;
  white-space: nowrap;
}

:deep(.global-header__menu.ant-menu) {
  flex: 1;
  min-width: 0;
  margin-left: 12px;
  background: transparent;
  border-bottom: none;
  color: var(--app-text-secondary);
}

:deep(.global-header__menu.ant-menu-horizontal) {
  justify-content: flex-start;
  line-height: 48px;
}

:deep(.global-header__menu .ant-menu-overflow) {
  flex-wrap: nowrap;
}

:deep(.global-header__menu.ant-menu-horizontal > .ant-menu-item) {
  padding-inline: 18px;
  font-weight: 500;
  margin-inline: 0;
}

:deep(.global-header__menu.ant-menu-horizontal > .ant-menu-item-selected) {
  color: var(--app-primary);
}

.global-header__actions {
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
}

@media (max-width: 991px) {
  .global-header {
    gap: 14px;
  }

  :deep(.global-header__menu.ant-menu-horizontal) {
    justify-content: flex-start;
  }

  .global-header__subtitle {
    display: none;
  }
}

@media (max-width: 767px) {
  .global-header {
    gap: 10px;
    padding: 10px 14px;
    border-radius: 18px;
  }

  .global-header__brand {
    gap: 10px;
  }

  .global-header__logo {
    width: 36px;
    height: 36px;
  }

  .global-header__title {
    font-size: 0.95rem;
  }

  :deep(.global-header__menu.ant-menu) {
    margin-left: 4px;
  }

  :deep(.global-header__menu.ant-menu-horizontal > .ant-menu-item) {
    padding-inline: 10px;
    font-size: 0.95rem;
  }

  :deep(.global-header__actions .ant-btn) {
    height: 40px;
    padding-inline: 16px;
  }
}

@media (max-width: 575px) {
  .global-header {
    gap: 8px;
    padding-inline: 12px;
  }

  .global-header__brand {
    gap: 8px;
  }

  .global-header__title {
    font-size: 0.9rem;
  }

  :deep(.global-header__menu.ant-menu) {
    margin-left: 0;
  }

  :deep(.global-header__menu.ant-menu-horizontal > .ant-menu-item) {
    padding-inline: 8px;
    font-size: 0.9rem;
  }

  :deep(.global-header__actions .ant-btn) {
    height: 36px;
    padding-inline: 14px;
  }
}
</style>
