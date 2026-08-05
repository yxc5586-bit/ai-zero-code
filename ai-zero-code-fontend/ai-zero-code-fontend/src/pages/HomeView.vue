<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { useRouter } from 'vue-router'

import AppCard from '@/components/AppCard.vue'
import { addApp, deleteApp, listGoodAppVoByPage, listMyAppVoByPage } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'
import { getApiErrorMessage, toSafePageNumber } from '@/utils/api'

type AppListState = {
  records: API.AppVO[]
  pageNum: number
  pageSize: number
  total: number
  appName: string
  loading: boolean
}

const PENDING_PROMPT_KEY = 'ai-zero-code:pending-prompt'
const router = useRouter()
const loginUserStore = useLoginUserStore()
const prompt = ref('')
const creating = ref(false)
const suggestions = [
  '设计一个简洁专业的企业官网',
  '生成一个展示技术文章的个人博客',
  '创建一个现代感电商商品落地页',
  '制作一个在线图片处理工具页面',
]

const createListState = (): AppListState => ({
  records: [], pageNum: 1, pageSize: 6, total: 0, appName: '', loading: false,
})
const myApps = reactive(createListState())
const featuredApps = reactive(createListState())

const fetchMyApps = async () => {
  if (!loginUserStore.isLoggedIn) return
  myApps.loading = true
  try {
    const response = await listMyAppVoByPage({
      pageNum: myApps.pageNum,
      pageSize: Math.min(myApps.pageSize, 20),
      appName: myApps.appName.trim() || undefined,
      sortField: 'createTime',
      sortOrder: 'descend',
    })
    if (response.data.code !== 0) throw new Error(response.data.message || '我的应用加载失败')
    myApps.records = response.data.data?.records ?? []
    myApps.total = toSafePageNumber(response.data.data?.totalRow)
  } catch (error) {
    message.error(getApiErrorMessage(error, '我的应用加载失败'))
  } finally {
    myApps.loading = false
  }
}

const fetchFeaturedApps = async () => {
  featuredApps.loading = true
  try {
    const response = await listGoodAppVoByPage({
      pageNum: featuredApps.pageNum,
      pageSize: Math.min(featuredApps.pageSize, 20),
      appName: featuredApps.appName.trim() || undefined,
      sortField: 'updateTime',
      sortOrder: 'descend',
    })
    if (response.data.code !== 0) throw new Error(response.data.message || '精选应用加载失败')
    featuredApps.records = response.data.data?.records ?? []
    featuredApps.total = toSafePageNumber(response.data.data?.totalRow)
  } catch (error) {
    message.error(getApiErrorMessage(error, '精选应用加载失败'))
  } finally {
    featuredApps.loading = false
  }
}

const createApplication = async () => {
  const value = prompt.value.trim()
  if (!value) {
    message.warning('先描述一下你想创建的网站')
    return
  }
  if (!loginUserStore.isLoggedIn) {
    sessionStorage.setItem(PENDING_PROMPT_KEY, value)
    await router.push({ name: 'login', query: { redirect: '/' } })
    return
  }

  creating.value = true
  try {
    const response = await addApp({ initPrompt: value })
    const appId = response.data.data
    if (response.data.code !== 0 || !appId) {
      message.error(response.data.message || '创建应用失败')
      return
    }
    sessionStorage.removeItem(PENDING_PROMPT_KEY)
    await router.push({ name: 'appChat', params: { id: appId }, query: { autoSend: '1' } })
  } catch (error) {
    message.error(getApiErrorMessage(error, '创建应用失败'))
  } finally {
    creating.value = false
  }
}

const chooseSuggestion = (value: string) => {
  prompt.value = value
}

const searchMyApps = () => { myApps.pageNum = 1; fetchMyApps() }
const searchFeaturedApps = () => { featuredApps.pageNum = 1; fetchFeaturedApps() }

const confirmDelete = (app: API.AppVO) => {
  Modal.confirm({
    title: `删除“${app.appName || '未命名应用'}”？`,
    content: '删除后无法恢复，请确认是否继续。',
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      if (!app.id) return
      const response = await deleteApp({ id: app.id })
      if (response.data.code !== 0 || !response.data.data) throw new Error(response.data.message || '删除失败')
      message.success('应用已删除')
      if (myApps.records.length === 1 && myApps.pageNum > 1) myApps.pageNum -= 1
      await fetchMyApps()
    },
  })
}

onMounted(async () => {
  await Promise.all([fetchFeaturedApps(), fetchMyApps()])
  const pendingPrompt = sessionStorage.getItem(PENDING_PROMPT_KEY)
  if (pendingPrompt && loginUserStore.isLoggedIn) {
    prompt.value = pendingPrompt
    sessionStorage.removeItem(PENDING_PROMPT_KEY)
    await nextTick()
    await createApplication()
  }
})
</script>

<template>
  <main class="home-page page-enter">
    <section class="hero">
      <div class="hero__brand-mark"><img src="/logo.png" alt="" /><span>AI 驱动的创作工作台</span></div>
      <h1>一句话，<em>生成你的专属网站</em></h1>
      <p>无需写代码。描述你的想法，AI 会帮你生成、预览、持续优化并一键部署。</p>
      <div class="prompt-studio">
        <a-textarea
          v-model:value="prompt"
          class="prompt-studio__input"
          :auto-size="{ minRows: 4, maxRows: 8 }"
          placeholder="例如：创建一个极简风格的个人博客，包含首页、文章列表、文章详情和关于我页面……"
          @keydown.ctrl.enter="createApplication"
          @keydown.meta.enter="createApplication"
        />
        <div class="prompt-studio__footer">
          <span>描述越具体，生成效果越准确</span>
          <a-button type="primary" size="large" :loading="creating" @click="createApplication">
            {{ creating ? '正在创建' : '开始生成' }} <span aria-hidden="true">↗</span>
          </a-button>
        </div>
      </div>
      <div class="suggestions" aria-label="推荐提示词">
        <button v-for="item in suggestions" :key="item" type="button" @click="chooseSuggestion(item)">{{ item }}</button>
      </div>
    </section>

    <div class="gallery-shell">
      <section class="app-section">
        <div class="section-heading">
          <div><span class="section-kicker">MY CREATIONS</span><h2>我的应用</h2><p>继续优化你创建的网站，或打开已经部署的作品。</p></div>
          <a-input-search v-if="loginUserStore.isLoggedIn" v-model:value="myApps.appName" class="section-search" placeholder="搜索我的应用" allow-clear @search="searchMyApps" />
        </div>
        <div v-if="!loginUserStore.isLoggedIn" class="signin-empty">
          <img src="/logo.png" alt="" />
          <div><h3>登录后查看你的创作</h3><p>所有应用都会保存在账户中，随时可以继续对话和部署。</p></div>
          <RouterLink to="/user/login"><a-button type="primary">去登录</a-button></RouterLink>
        </div>
        <a-spin v-else :spinning="myApps.loading">
          <div v-if="myApps.records.length" class="app-grid">
            <AppCard v-for="app in myApps.records" :key="app.id" :app="app" editable @delete="confirmDelete" />
          </div>
          <a-empty v-else-if="!myApps.loading" description="还没有应用，从上方描述一个想法开始吧" />
          <div v-if="myApps.total > myApps.pageSize" class="pagination-row">
            <a-pagination v-model:current="myApps.pageNum" v-model:page-size="myApps.pageSize" :total="myApps.total" :page-size-options="['6','12','18']" show-size-changer @change="fetchMyApps" @show-size-change="fetchMyApps" />
          </div>
        </a-spin>
      </section>

      <section class="app-section app-section--featured">
        <div class="section-heading">
          <div><span class="section-kicker">CURATED SHOWCASE</span><h2>精选应用</h2><p>看看大家用一句话创造出的优秀网站。</p></div>
          <a-input-search v-model:value="featuredApps.appName" class="section-search" placeholder="搜索精选应用" allow-clear @search="searchFeaturedApps" />
        </div>
        <a-spin :spinning="featuredApps.loading">
          <div v-if="featuredApps.records.length" class="app-grid">
            <AppCard v-for="app in featuredApps.records" :key="app.id" :app="app" />
          </div>
          <a-empty v-else-if="!featuredApps.loading" description="暂时还没有精选应用" />
          <div v-if="featuredApps.total > featuredApps.pageSize" class="pagination-row">
            <a-pagination v-model:current="featuredApps.pageNum" v-model:page-size="featuredApps.pageSize" :total="featuredApps.total" :page-size-options="['6','12','18']" show-size-changer @change="fetchFeaturedApps" @show-size-change="fetchFeaturedApps" />
          </div>
        </a-spin>
      </section>
    </div>
  </main>
</template>

<style scoped>
.home-page { padding-bottom: 70px; }
.hero { position: relative; display: flex; flex-direction: column; align-items: center; min-height: 650px; padding: 94px 24px 118px; overflow: hidden; text-align: center; }
.hero::after { position: absolute; left: 50%; bottom: -320px; width: 1040px; height: 560px; content: ''; transform: translateX(-50%); background: radial-gradient(ellipse, rgba(23,105,224,.25), transparent 67%); filter: blur(20px); }
.hero__brand-mark { display: inline-flex; align-items: center; gap: 9px; padding: 6px 13px 6px 7px; color: #176154; font-size: 12px; font-weight: 700; background: rgba(255,255,255,.72); border: 1px solid rgba(16,185,129,.17); border-radius: 999px; }
.hero__brand-mark img { width: 28px; height: 28px; object-fit: cover; border-radius: 50%; }
.hero h1 { max-width: 900px; margin: 28px 0 18px; color: var(--app-ink); font-size: clamp(42px, 6vw, 76px); line-height: 1.08; letter-spacing: -.055em; }
.hero h1 em { color: var(--app-primary-deep); font-style: normal; }
.hero > p { max-width: 720px; margin: 0; color: var(--app-muted); font-size: 17px; }
.prompt-studio { position: relative; z-index: 2; width: min(100%, 1000px); margin-top: 44px; padding: 14px; background: rgba(255,255,255,.9); border: 1px solid rgba(255,255,255,.98); border-radius: 26px; box-shadow: var(--app-shadow); backdrop-filter: blur(20px); }
:deep(.prompt-studio__input.ant-input) { padding: 18px 18px 8px; font-size: 16px; line-height: 1.8; background: transparent; border: 0; box-shadow: none; resize: none; }
.prompt-studio__footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 10px 10px 2px 16px; border-top: 1px solid rgba(15,42,68,.07); }
.prompt-studio__footer > span { color: #8a96a8; font-size: 12px; }
.prompt-studio__footer :deep(.ant-btn-primary) { height: 46px; padding-inline: 22px; font-weight: 700; background: var(--app-primary); border-radius: 13px; }
.suggestions { position: relative; z-index: 2; display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; margin-top: 22px; }
.suggestions button { padding: 9px 14px; color: #5f6b7a; background: rgba(255,255,255,.66); border: 1px solid rgba(15,42,68,.08); border-radius: 999px; cursor: pointer; transition: var(--app-transition); }
.suggestions button:hover { color: var(--app-primary-deep); border-color: rgba(16,185,129,.32); background: #fff; transform: translateY(-1px); }
.gallery-shell { position: relative; z-index: 3; width: min(100% - 32px, var(--app-content-width)); margin: -50px auto 0; padding: 50px clamp(22px,4vw,52px) 58px; background: rgba(255,255,255,.89); border: 1px solid rgba(255,255,255,.94); border-radius: 34px; box-shadow: var(--app-shadow); backdrop-filter: blur(18px); }
.app-section + .app-section { margin-top: 72px; padding-top: 64px; border-top: 1px solid var(--app-border); }
.section-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 28px; margin-bottom: 28px; }
.section-kicker { color: var(--app-primary-deep); font-size: 11px; font-weight: 800; letter-spacing: .18em; }
.section-heading h2 { margin: 4px 0 3px; color: var(--app-ink); font-size: 30px; letter-spacing: -.035em; }
.section-heading p { margin: 0; color: var(--app-muted); }
.section-search { width: 250px; }
.app-grid { display: grid; grid-template-columns: repeat(3, minmax(0,1fr)); gap: 24px; }
.signin-empty { display: flex; align-items: center; gap: 18px; padding: 28px; background: linear-gradient(120deg, #effaf6, #edf5ff); border: 1px solid var(--app-border); border-radius: var(--app-radius-md); }
.signin-empty img { width: 58px; height: 58px; object-fit: cover; border-radius: 50%; }
.signin-empty div { flex: 1; }
.signin-empty h3 { margin: 0 0 4px; }
.signin-empty p { margin: 0; color: var(--app-muted); }
.pagination-row { display: flex; justify-content: center; margin-top: 34px; }
@media (max-width: 980px) { .app-grid { grid-template-columns: repeat(2,minmax(0,1fr)); } }
@media (max-width: 700px) {
  .hero { min-height: 620px; padding: 70px 16px 96px; }
  .hero h1 { font-size: clamp(36px, 10.5vw, 44px); }
  .hero h1 em { display: block; margin-top: 6px; }
  .prompt-studio { margin-top: 34px; border-radius: 20px; }
  .prompt-studio__footer { align-items: flex-end; }
  .prompt-studio__footer > span { display: none; }
  .prompt-studio__footer :deep(.ant-btn) { width: 100%; }
  .gallery-shell { width: min(100% - 20px, var(--app-content-width)); padding: 34px 16px 42px; border-radius: 25px; }
  .section-heading { align-items: stretch; flex-direction: column; gap: 16px; }
  .section-search { width: 100%; }
  .app-grid { grid-template-columns: 1fr; }
  .signin-empty { align-items: flex-start; flex-wrap: wrap; }
}
</style>
