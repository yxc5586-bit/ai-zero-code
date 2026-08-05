import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import router from '@/router'

router.beforeEach(async (to, from, next) => {
  // 1. 获取当前登录用户
  const loginUser = useLoginUserStore().loginUser

  // 2. 判断用户是否已登录
  const isLogin = loginUser && loginUser.id

  // 3. 需要登录才能访问的页面（白名单除外）
  const requireAuth = !['/user/login', '/user/register'].includes(to.path)

  if (requireAuth && !isLogin) {
    // 未登录 → 跳转登录页，登录后可以回到当前页
    message.warning('请先登录')
    next(`/user/login?redirect=${to.fullPath}`)
    return
  }

  // 4. 管理员权限校验（访问 /admin 开头的页面）
  if (to.path.startsWith('/admin')) {
    if (!isLogin || loginUser.userRole !== 'admin') {
      // ❌ 没有管理员权限
      message.error('您没有管理员权限，将跳转至首页')
      next('/')  // 👈 跳转到首页
      return
    }
  }

  // ✅ 放行
  next()
})
