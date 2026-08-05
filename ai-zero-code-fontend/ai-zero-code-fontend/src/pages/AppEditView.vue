<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'

import {
  getAppVoById,
  getAppVoByIdByAdmin,
  updateAppByAdmin,
  updateMyApp,
} from '@/api/appController'
import AppCover from '@/components/AppCover.vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { getApiErrorMessage } from '@/utils/api'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const appId = computed(() => String(route.params.id ?? ''))
const loading = ref(true)
const submitting = ref(false)
const loadError = ref('')
const form = reactive({ appName: '', cover: '', priority: 0 })
const codeGenType = ref('multi_file')
const previewApp = computed(() => ({
  appName: form.appName,
  cover: form.cover,
  codeGenType: codeGenType.value,
}))

const backToSource = () => {
  if (route.query.from === 'admin') router.push({ name: 'adminApp' })
  else router.push({ name: 'home' })
}

const loadApp = async () => {
  loading.value = true
  try {
    const response = loginUserStore.isAdmin
      ? await getAppVoByIdByAdmin({ id: appId.value })
      : await getAppVoById({ id: appId.value })
    const app = response.data.data
    if (response.data.code !== 0 || !app) {
      loadError.value = response.data.message || '应用不存在'
      return
    }
    if (!loginUserStore.isAdmin && app.userId !== loginUserStore.loginUser?.id) {
      loadError.value = '你只能修改自己创建的应用'
      return
    }
    form.appName = app.appName ?? ''
    form.cover = app.cover ?? ''
    form.priority = app.priority ?? 0
    codeGenType.value = app.codeGenType ?? 'multi_file'
  } catch (error) {
    loadError.value = getApiErrorMessage(error, '应用加载失败')
  } finally {
    loading.value = false
  }
}

const submitForm = async () => {
  const appName = form.appName.trim()
  if (!appName) {
    message.warning('应用名称不能为空')
    return
  }
  submitting.value = true
  try {
    const response = loginUserStore.isAdmin
      ? await updateAppByAdmin({
          id: appId.value,
          appName,
          cover: form.cover.trim() || undefined,
          priority: form.priority,
        })
      : await updateMyApp({ id: appId.value, appName })
    if (response.data.code !== 0 || !response.data.data) {
      message.error(response.data.message || '保存失败')
      return
    }
    message.success('应用信息已更新')
    backToSource()
  } catch (error) {
    message.error(getApiErrorMessage(error, '保存失败'))
  } finally {
    submitting.value = false
  }
}

onMounted(loadApp)
</script>

<template>
  <main class="edit-page page-enter">
    <div class="edit-page__heading">
      <div>
        <span>APP SETTINGS</span>
        <h1>修改应用信息</h1>
        <p>
          {{
            loginUserStore.isAdmin
              ? '管理员可以维护名称、封面和应用优先级。'
              : '为你的应用设置一个清晰、易识别的名称。'
          }}
        </p>
      </div>
      <a-button @click="backToSource">返回</a-button>
    </div>

    <a-spin :spinning="loading">
      <section v-if="loadError" class="edit-card">
        <a-result status="403" title="无法编辑应用" :sub-title="loadError"
          ><template #extra
            ><a-button type="primary" @click="backToSource">返回上一页</a-button></template
          ></a-result
        >
      </section>
      <section v-else class="edit-card">
        <div class="edit-form">
          <a-form layout="vertical" @finish="submitForm">
            <a-form-item label="应用名称" required extra="名称会显示在主页卡片和生成工作台顶部。">
              <a-input
                v-model:value="form.appName"
                size="large"
                placeholder="请输入应用名称"
                :maxlength="40"
                show-count
              />
            </a-form-item>
            <template v-if="loginUserStore.isAdmin">
              <a-form-item
                label="封面 URL"
                extra="当前后端未提供文件上传接口，请填写可公开访问的图片地址。"
              >
                <a-input
                  v-model:value="form.cover"
                  size="large"
                  placeholder="https://example.com/cover.png"
                />
              </a-form-item>
              <a-form-item label="优先级" extra="优先级为 99 时，该应用会出现在精选应用列表。">
                <a-input-number
                  v-model:value="form.priority"
                  size="large"
                  :min="0"
                  :max="999"
                  style="width: 100%"
                />
              </a-form-item>
            </template>
            <div class="form-actions">
              <a-button @click="backToSource">取消</a-button
              ><a-button type="primary" html-type="submit" :loading="submitting">保存修改</a-button>
            </div>
          </a-form>
        </div>
        <aside class="cover-preview">
          <span>封面预览</span>
          <div class="cover-preview__image">
            <AppCover :app="previewApp" />
          </div>
          <p>推荐使用 16:9 横向图片，列表中会自动裁切显示。</p>
        </aside>
      </section>
    </a-spin>
  </main>
</template>

<style scoped>
.edit-page {
  width: min(100% - 32px, 1080px);
  margin: 58px auto 90px;
}
.edit-page__heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 28px;
}
.edit-page__heading span {
  color: var(--app-primary-deep);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.18em;
}
.edit-page__heading h1 {
  margin: 5px 0 6px;
  color: var(--app-ink);
  font-size: 36px;
  letter-spacing: -0.04em;
}
.edit-page__heading p {
  margin: 0;
  color: var(--app-muted);
}
.edit-card {
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(280px, 0.88fr);
  gap: 50px;
  min-height: 420px;
  padding: clamp(28px, 5vw, 54px);
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.96);
  border-radius: var(--app-radius-lg);
  box-shadow: var(--app-shadow);
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 34px;
}
.form-actions :deep(.ant-btn-primary) {
  background: var(--app-primary);
}
.cover-preview > span {
  display: block;
  margin-bottom: 10px;
  color: var(--app-muted);
  font-size: 13px;
}
.cover-preview__image {
  aspect-ratio: 16/9;
  overflow: hidden;
  background: #e9f5f4;
  border: 1px solid var(--app-border);
  border-radius: 16px;
  box-shadow: var(--app-shadow-soft);
}
.cover-preview > p {
  color: var(--app-muted);
  font-size: 12px;
}
@media (max-width: 760px) {
  .edit-page {
    margin-top: 36px;
  }
  .edit-page__heading {
    align-items: flex-start;
    flex-direction: column;
  }
  .edit-card {
    grid-template-columns: 1fr;
    gap: 34px;
    padding: 26px 20px;
  }
  .cover-preview {
    order: -1;
  }
}
</style>
