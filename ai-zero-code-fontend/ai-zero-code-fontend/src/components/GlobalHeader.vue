<script setup lang="ts">
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'

import { useLoginUserStore } from '@/stores/loginUser'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const menuItems = computed(() => {
  const items = [{ key: 'home', label: '首页', path: '/' }]
  if (loginUserStore.isAdmin) {
    items.push(
      { key: 'adminApp', label: '应用管理', path: '/admin/app' },
      { key: 'adminUser', label: '用户管理', path: '/admin/user' },
    )
  }
  return items
})

const selectedKeys = computed(() => {
  if (route.path.startsWith('/admin/app')) return ['adminApp']
  if (route.path.startsWith('/admin/user')) return ['adminUser']
  return route.path === '/' ? ['home'] : []
})

const displayName = computed(
  () => loginUserStore.loginUser?.userName || loginUserStore.loginUser?.userAccount || '用户',
)

const handleMenuClick = ({ key }: { key: string }) => {
  const target = menuItems.value.find((item) => item.key === key)
  if (target) router.push(target.path)
}

const handleLogout = async () => {
  try {
    const result = await loginUserStore.logout()
    if (result.code === 0) {
      message.success('已退出登录')
      await router.push('/')
    } else {
      message.error(result.message || '退出失败')
    }
  } catch {
    message.error('退出失败，请稍后重试')
  }
}
</script>

<template>
  <div class="global-header">
    <RouterLink class="brand" to="/" aria-label="返回首页">
      <img class="brand__logo" src="/logo.png" alt="AI 零代码平台熊猫标识" />
      <div class="brand__copy">
        <strong>AI 零代码平台</strong>
        <span>一句话，让想法成为网站</span>
      </div>
    </RouterLink>

    <a-menu
      mode="horizontal"
      class="global-header__menu"
      :selected-keys="selectedKeys"
      @click="handleMenuClick"
    >
      <a-menu-item v-for="item in menuItems" :key="item.key">{{ item.label }}</a-menu-item>
    </a-menu>

    <div class="global-header__actions">
      <template v-if="loginUserStore.isLoggedIn">
        <a-dropdown placement="bottomRight">
          <button class="user-button" type="button">
            <a-avatar :size="34" :src="loginUserStore.loginUser?.userAvatar">
              {{ displayName.slice(0, 1).toUpperCase() }}
            </a-avatar>
            <span class="user-button__name">{{ displayName }}</span>
            <span aria-hidden="true">⌄</span>
          </button>
          <template #overlay>
            <a-menu>
              <a-menu-item key="home" @click="router.push('/')">我的应用</a-menu-item>
              <template v-if="loginUserStore.isAdmin">
                <a-menu-item key="adminApp" @click="router.push('/admin/app')"
                  >应用管理</a-menu-item
                >
                <a-menu-item key="adminUser" @click="router.push('/admin/user')"
                  >用户管理</a-menu-item
                >
              </template>
              <a-menu-divider />
              <a-menu-item key="logout" @click="handleLogout">退出登录</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </template>
      <template v-else>
        <RouterLink to="/user/login"><a-button>登录</a-button></RouterLink>
        <RouterLink class="register-link" to="/user/register"
          ><a-button type="primary">注册</a-button></RouterLink
        >
      </template>
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
}
.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.brand__logo {
  width: 42px;
  height: 42px;
  object-fit: cover;
  border: 3px solid rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  box-shadow: 0 8px 24px rgba(14, 116, 94, 0.18);
}
.brand__copy {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}
.brand__copy strong {
  color: var(--app-ink);
  font-size: 16px;
  letter-spacing: 0.02em;
}
.brand__copy span {
  margin-top: 3px;
  color: var(--app-muted);
  font-size: 11px;
}
:deep(.global-header__menu.ant-menu) {
  flex: 1;
  min-width: 0;
  background: transparent;
  border: 0;
}
:deep(.global-header__menu.ant-menu-horizontal) {
  line-height: 50px;
}
:deep(.global-header__menu.ant-menu-horizontal > .ant-menu-item) {
  padding-inline: 18px;
  font-weight: 600;
}
:deep(.global-header__menu.ant-menu-horizontal > .ant-menu-item::after) {
  border-bottom-width: 3px;
  border-bottom-color: var(--app-primary);
}
.global-header__actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 8px;
}
.user-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px 4px 4px;
  color: var(--app-text);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--app-border);
  border-radius: 999px;
  cursor: pointer;
  transition: var(--app-transition);
}
.user-button:hover {
  border-color: rgba(16, 185, 129, 0.35);
  background: #fff;
}
@media (max-width: 767px) {
  .global-header {
    gap: 8px;
  }
  .brand__copy span,
  .user-button__name {
    display: none;
  }
  :deep(.global-header__menu.ant-menu-horizontal > .ant-menu-item) {
    padding-inline: 10px;
  }
  .register-link {
    display: none;
  }
}
@media (max-width: 620px) {
  :deep(.global-header__menu.ant-menu) {
    display: none;
  }
  .global-header__actions {
    margin-left: auto;
  }
}
@media (max-width: 480px) {
  .brand__copy strong {
    display: none;
  }
  .brand__logo {
    width: 38px;
    height: 38px;
  }
}
</style>
