import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { getLoginUser, userLogin, userLogout, userRegister } from '@/api/userController'

export const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<API.LoginUserVO | null>(null)
  const initialized = ref(false)
  const loading = ref(false)

  const isLoggedIn = computed(() => Boolean(loginUser.value?.id))
  const isAdmin = computed(() => loginUser.value?.userRole === 'admin')

  const fetchLoginUser = async (force = false) => {
    if (initialized.value && !force) {
      return loginUser.value
    }

    loading.value = true
    try {
      const response = await getLoginUser()
      loginUser.value = response.data.code === 0 ? (response.data.data ?? null) : null
    } catch {
      loginUser.value = null
    } finally {
      initialized.value = true
      loading.value = false
    }

    return loginUser.value
  }

  const login = async (payload: API.UserLoginRequest) => {
    const response = await userLogin(payload)
    if (response.data.code === 0 && response.data.data) {
      loginUser.value = response.data.data
      initialized.value = true
    }
    return response.data
  }

  const register = async (payload: API.UserRegisterRequest) => {
    const response = await userRegister(payload)
    return response.data
  }

  const logout = async () => {
    const response = await userLogout()
    if (response.data.code === 0 && response.data.data) {
      loginUser.value = null
      initialized.value = true
    }
    return response.data
  }

  return {
    loginUser,
    initialized,
    loading,
    isLoggedIn,
    isAdmin,
    fetchLoginUser,
    login,
    register,
    logout,
  }
})
