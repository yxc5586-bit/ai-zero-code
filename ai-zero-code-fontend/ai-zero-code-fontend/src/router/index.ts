import { createRouter, createWebHistory } from 'vue-router'
import { message } from 'ant-design-vue'

import BasicLayout from '@/layouts/BasicLayout.vue'
import pinia from '@/stores'
import { useLoginUserStore } from '@/stores/loginUser'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/',
      component: BasicLayout,
      children: [
        { path: '', name: 'home', component: () => import('@/pages/HomeView.vue') },
        {
          path: 'user/login',
          name: 'login',
          component: () => import('@/pages/AuthView.vue'),
          props: { mode: 'login' },
        },
        {
          path: 'user/register',
          name: 'register',
          component: () => import('@/pages/AuthView.vue'),
          props: { mode: 'register' },
        },
        {
          path: 'app/edit/:id',
          name: 'appEdit',
          component: () => import('@/pages/AppEditView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'admin/app',
          name: 'adminApp',
          component: () => import('@/pages/AdminAppView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true },
        },
        {
          path: 'admin/user',
          name: 'adminUser',
          component: () => import('@/pages/AdminUserView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true },
        },
        {
          path: 'admin/userManage',
          redirect: { name: 'adminUser' },
        },
        {
          path: '403',
          name: 'forbidden',
          component: () => import('@/pages/StatusView.vue'),
          props: { status: '403' },
        },
        {
          path: ':pathMatch(.*)*',
          name: 'notFound',
          component: () => import('@/pages/StatusView.vue'),
          props: { status: '404' },
        },
      ],
    },
    {
      path: '/app/chat/:id',
      name: 'appChat',
      component: () => import('@/pages/AppChatView.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach(async (to) => {
  const loginUserStore = useLoginUserStore(pinia)
  await loginUserStore.fetchLoginUser()

  if (to.meta.requiresAuth && !loginUserStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAdmin && !loginUserStore.isAdmin) {
    message.error('你没有访问该页面的权限')
    return { name: 'forbidden' }
  }

  if ((to.name === 'login' || to.name === 'register') && loginUserStore.isLoggedIn) {
    return { name: 'home' }
  }

  return true
})

export default router
