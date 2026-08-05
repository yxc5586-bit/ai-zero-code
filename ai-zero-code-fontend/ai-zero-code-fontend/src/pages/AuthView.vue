<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'

import { useLoginUserStore } from '@/stores/loginUser'
import { getApiErrorMessage } from '@/utils/api'

const props = defineProps<{ mode: 'login' | 'register' }>()
const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const submitting = ref(false)
const form = reactive({ userAccount: '', userPassword: '', checkPassword: '' })
const REGISTERED_ACCOUNT_KEY = 'ai-zero-code:registered-account'

const isLogin = computed(() => props.mode === 'login')
const title = computed(() => (isLogin.value ? '欢迎回来' : '创建你的账户'))
const subtitle = computed(() =>
  isLogin.value ? '登录后继续创建和管理你的应用' : '注册后即可用一句话生成网站',
)

const getRedirectTarget = () => {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
  return redirect.startsWith('/') ? redirect : '/'
}

watch(
  () => props.mode,
  (mode) => {
    form.userPassword = ''
    form.checkPassword = ''
    if (mode === 'login') {
      const registeredAccount = sessionStorage.getItem(REGISTERED_ACCOUNT_KEY)
      if (registeredAccount) {
        form.userAccount = registeredAccount
        sessionStorage.removeItem(REGISTERED_ACCOUNT_KEY)
      }
    }
  },
  { immediate: true },
)

const handleSubmit = async () => {
  const account = form.userAccount.trim()
  if (!account || !form.userPassword) {
    message.warning('请输入账号和密码')
    return
  }
  if (!isLogin.value && form.userPassword !== form.checkPassword) {
    message.warning('两次输入的密码不一致')
    return
  }

  submitting.value = true
  try {
    if (isLogin.value) {
      const result = await loginUserStore.login({ userAccount: account, userPassword: form.userPassword })
      if (result.code !== 0) {
        message.error(result.message || '登录失败')
        return
      }
      message.success('登录成功')
      await router.replace(getRedirectTarget())
      return
    }

    const result = await loginUserStore.register({
      userAccount: account,
      userPassword: form.userPassword,
      checkPassword: form.checkPassword,
    })
    if (result.code !== 0) {
      message.error(result.message || '注册失败')
      return
    }
    message.success('注册成功，请登录')
    sessionStorage.setItem(REGISTERED_ACCOUNT_KEY, account)
    await router.replace({ name: 'login', query: route.query })
  } catch (error) {
    message.error(getApiErrorMessage(error, isLogin.value ? '登录失败' : '注册失败'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="auth-page page-enter">
    <section class="auth-card">
      <div class="auth-card__visual">
        <img src="/logo.png" alt="熊猫品牌标识" />
        <p class="eyebrow">AI ZEROCODE</p>
        <h1>把灵感说出来，<br />剩下的交给 AI。</h1>
        <p>从一句描述，到可预览、可继续优化、可部署的网站。</p>
        <div class="visual-orbit" aria-hidden="true"><span></span><span></span><span></span></div>
      </div>
      <div class="auth-card__form">
        <p class="eyebrow">{{ isLogin ? 'SIGN IN' : 'GET STARTED' }}</p>
        <h2>{{ title }}</h2>
        <p class="auth-subtitle">{{ subtitle }}</p>
        <form class="auth-form" @submit.prevent="handleSubmit">
          <a-form-item label="账号" required>
            <a-input v-model:value="form.userAccount" size="large" placeholder="请输入账号" autocomplete="username" />
          </a-form-item>
          <a-form-item label="密码" required>
            <a-input-password v-model:value="form.userPassword" size="large" placeholder="请输入密码" :autocomplete="isLogin ? 'current-password' : 'new-password'" />
          </a-form-item>
          <a-form-item v-if="!isLogin" label="确认密码" required>
            <a-input-password v-model:value="form.checkPassword" size="large" placeholder="请再次输入密码" autocomplete="new-password" />
          </a-form-item>
          <a-button class="auth-submit" type="primary" html-type="submit" size="large" :loading="submitting">
            {{ isLogin ? '登录并继续' : '创建账户' }}
          </a-button>
        </form>
        <p class="auth-switch">
          {{ isLogin ? '还没有账户？' : '已经有账户？' }}
          <RouterLink :to="isLogin ? '/user/register' : '/user/login'">{{ isLogin ? '立即注册' : '返回登录' }}</RouterLink>
        </p>
      </div>
    </section>
  </main>
</template>

<style scoped>
.auth-page { width: min(100% - 32px, 1040px); margin: 54px auto 80px; }
.auth-card { display: grid; grid-template-columns: .95fr 1.05fr; min-height: 590px; overflow: hidden; background: rgba(255,255,255,.93); border: 1px solid rgba(255,255,255,.88); border-radius: 32px; box-shadow: var(--app-shadow); }
.auth-card__visual { position: relative; overflow: hidden; padding: 62px 54px; color: #fff; background: linear-gradient(150deg, #0b6b61 0%, #0f9f86 46%, #1769e0 120%); }
.auth-card__visual::after { position: absolute; inset: auto -20% -30% 20%; width: 420px; height: 420px; content: ''; border: 1px solid rgba(255,255,255,.18); border-radius: 50%; box-shadow: 0 0 0 58px rgba(255,255,255,.05), 0 0 0 116px rgba(255,255,255,.035); }
.auth-card__visual img { width: 72px; height: 72px; margin-bottom: 46px; object-fit: cover; border: 4px solid rgba(255,255,255,.82); border-radius: 50%; }
.eyebrow { margin: 0 0 14px; font-size: 12px; font-weight: 800; letter-spacing: .18em; }
.auth-card__visual h1 { position: relative; z-index: 1; margin: 0; font-size: clamp(36px, 4vw, 54px); line-height: 1.14; letter-spacing: -.045em; }
.auth-card__visual > p:last-of-type { position: relative; z-index: 1; max-width: 340px; margin-top: 26px; color: rgba(255,255,255,.8); font-size: 16px; line-height: 1.8; }
.visual-orbit { position: absolute; right: 44px; bottom: 42px; z-index: 2; display: flex; gap: 8px; }
.visual-orbit span { width: 9px; height: 9px; background: rgba(255,255,255,.4); border-radius: 50%; }
.visual-orbit span:first-child { width: 30px; border-radius: 999px; background: #fff; }
.auth-card__form { display: flex; flex-direction: column; justify-content: center; padding: 60px clamp(36px, 6vw, 76px); }
.auth-card__form .eyebrow { color: var(--app-primary-deep); }
.auth-card__form h2 { margin: 0; color: var(--app-ink); font-size: 34px; letter-spacing: -.03em; }
.auth-subtitle { margin: 10px 0 34px; color: var(--app-muted); }
.auth-submit { width: 100%; height: 48px; margin-top: 8px; background: var(--app-primary); }
.auth-switch { margin: 26px 0 0; color: var(--app-muted); text-align: center; }
.auth-switch a { color: var(--app-blue); font-weight: 700; }
:deep(.ant-input-affix-wrapper), :deep(.ant-input) { border-radius: 10px; }
@media (max-width: 800px) {
  .auth-card { grid-template-columns: 1fr; }
  .auth-card__visual { min-height: 300px; padding: 38px; }
  .auth-card__visual img { width: 58px; height: 58px; margin-bottom: 24px; }
  .auth-card__visual h1 { font-size: 38px; }
  .auth-card__form { padding: 42px 30px; }
}
</style>
