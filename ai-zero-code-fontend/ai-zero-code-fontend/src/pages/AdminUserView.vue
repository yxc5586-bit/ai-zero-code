<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { Modal, message, type FormInstance } from 'ant-design-vue'

import { createUser, deleteUser, listUser, updateUser } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'
import { getApiErrorMessage, toSafePageNumber } from '@/utils/api'
import { formatDateTime } from '@/utils/format'

type EditorMode = 'create' | 'edit'

const loginUserStore = useLoginUserStore()
const loading = ref(false)
const submitting = ref(false)
const records = ref<API.UserVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const detailUser = ref<API.UserVO | null>(null)
const editorVisible = ref(false)
const editorMode = ref<EditorMode>('create')
const userFormRef = ref<FormInstance>()

const filters = reactive<API.UserQueryRequest>({
  id: undefined,
  userAccount: '',
  userName: '',
  userProfile: '',
  userRole: undefined,
})

const editorForm = reactive<API.UserAddRequest & { id?: string }>({
  id: undefined,
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
})

const columns = [
  { title: '用户', key: 'user', width: 270, fixed: 'left' },
  { title: '用户 ID', dataIndex: 'id', key: 'id', width: 190 },
  { title: '角色', dataIndex: 'userRole', key: 'userRole', width: 110 },
  { title: '个人简介', dataIndex: 'userProfile', key: 'userProfile', width: 280 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 190, fixed: 'right' },
]

const editorRules = computed(() => ({
  userAccount:
    editorMode.value === 'create'
      ? [
          { required: true, message: '请输入用户账号', trigger: 'blur' },
          { min: 4, message: '账号长度不能少于 4 位', trigger: 'blur' },
        ]
      : [],
  userName: [{ required: true, message: '请输入用户昵称', trigger: 'blur' }],
  userRole: [{ required: true, message: '请选择用户角色', trigger: 'change' }],
  userAvatar: [{ type: 'url' as const, message: '请输入完整的头像 URL', trigger: 'blur' }],
}))

const isEditingSelf = computed(
  () => editorMode.value === 'edit' && editorForm.id === loginUserStore.loginUser?.id,
)

const currentPageAdminCount = computed(
  () => records.value.filter((user) => user.userRole === 'admin').length,
)

const cleanFilters = computed(() => {
  const result: API.UserQueryRequest = {}
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== '' && value !== undefined && value !== null) {
      ;(result as Record<string, unknown>)[key] = typeof value === 'string' ? value.trim() : value
    }
  })
  return result
})

const getUserDisplayName = (user: API.UserVO) => user.userName || user.userAccount || '未命名用户'

const getRoleMeta = (role?: string) => {
  if (role === 'admin') return { label: '管理员', color: 'green' }
  if (role === 'ban') return { label: '已封禁', color: 'red' }
  return { label: '普通用户', color: 'blue' }
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const response = await listUser({
      ...cleanFilters.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      sortField: 'createTime',
      sortOrder: 'descend',
    })
    if (response.data.code !== 0) {
      throw new Error(response.data.message || '用户列表加载失败')
    }
    records.value = response.data.data?.records ?? []
    total.value = toSafePageNumber(response.data.data?.totalRow)
  } catch (error) {
    records.value = []
    message.error(getApiErrorMessage(error, '用户列表加载失败'))
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  void fetchUsers()
}

const resetFilters = () => {
  Object.assign(filters, {
    id: undefined,
    userAccount: '',
    userName: '',
    userProfile: '',
    userRole: undefined,
  })
  pageNum.value = 1
  void fetchUsers()
}

const handlePageChange = (nextPage: number, nextPageSize: number) => {
  if (nextPageSize !== pageSize.value) {
    pageSize.value = nextPageSize
    pageNum.value = 1
  } else {
    pageNum.value = nextPage
  }
  void fetchUsers()
}

const resetEditorForm = () => {
  Object.assign(editorForm, {
    id: undefined,
    userAccount: '',
    userName: '',
    userAvatar: '',
    userProfile: '',
    userRole: 'user',
  })
}

const openCreate = async () => {
  editorMode.value = 'create'
  resetEditorForm()
  editorVisible.value = true
  await nextTick()
  userFormRef.value?.clearValidate()
}

const openEdit = async (user: API.UserVO) => {
  editorMode.value = 'edit'
  Object.assign(editorForm, {
    id: user.id,
    userAccount: user.userAccount ?? '',
    userName: user.userName ?? '',
    userAvatar: user.userAvatar ?? '',
    userProfile: user.userProfile ?? '',
    userRole: user.userRole || 'user',
  })
  editorVisible.value = true
  await nextTick()
  userFormRef.value?.clearValidate()
}

const submitEditor = async () => {
  try {
    await userFormRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (editorMode.value === 'create') {
      const response = await createUser({
        userAccount: editorForm.userAccount?.trim(),
        userName: editorForm.userName?.trim(),
        userAvatar: editorForm.userAvatar?.trim(),
        userProfile: editorForm.userProfile?.trim(),
        userRole: editorForm.userRole,
      })
      if (response.data.code !== 0 || !response.data.data) {
        throw new Error(response.data.message || '创建用户失败')
      }
      message.success('用户已创建，初始密码为 12345678')
      pageNum.value = 1
    } else {
      const response = await updateUser({
        id: editorForm.id,
        userName: editorForm.userName?.trim(),
        userAvatar: editorForm.userAvatar?.trim(),
        userProfile: editorForm.userProfile?.trim(),
        userRole: editorForm.userRole,
      })
      if (response.data.code !== 0 || !response.data.data) {
        throw new Error(response.data.message || '更新用户失败')
      }
      message.success('用户信息已更新')
      if (editorForm.id === loginUserStore.loginUser?.id) {
        await loginUserStore.fetchLoginUser(true)
      }
    }

    editorVisible.value = false
    await fetchUsers()
  } catch (error) {
    message.error(
      getApiErrorMessage(error, editorMode.value === 'create' ? '创建用户失败' : '更新用户失败'),
    )
  } finally {
    submitting.value = false
  }
}

const openDetail = (user: API.UserVO) => {
  detailUser.value = user
  detailVisible.value = true
}

const editDetailUser = () => {
  if (!detailUser.value) return
  const user = detailUser.value
  detailVisible.value = false
  void openEdit(user)
}

const confirmDelete = (user: API.UserVO) => {
  if (!user.id || user.id === loginUserStore.loginUser?.id) return

  Modal.confirm({
    title: `删除用户“${getUserDisplayName(user)}”？`,
    content: `用户账号：${user.userAccount || '—'}。删除后该用户将无法继续登录，此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        const response = await deleteUser({ id: user.id })
        if (response.data.code !== 0 || !response.data.data) {
          throw new Error(response.data.message || '删除用户失败')
        }
        message.success('用户已删除')
        if (records.value.length === 1 && pageNum.value > 1) pageNum.value -= 1
        await fetchUsers()
      } catch (error) {
        message.error(getApiErrorMessage(error, '删除用户失败'))
        throw error
      }
    },
  })
}

onMounted(fetchUsers)
</script>

<template>
  <main class="admin-page page-enter">
    <div class="admin-heading">
      <div>
        <span>ADMIN CONSOLE</span>
        <h1>用户管理</h1>
        <p>维护平台账号、公开资料与管理员权限。</p>
      </div>
      <div class="admin-heading__actions">
        <div class="admin-heading__stat" aria-live="polite">
          <strong>{{ total }}</strong>
          <span>位用户</span>
          <small>本页 {{ currentPageAdminCount }} 位管理员</small>
        </div>
        <a-button type="primary" size="large" @click="openCreate">新增用户</a-button>
      </div>
    </div>

    <section class="filter-card" aria-label="用户筛选">
      <a-form layout="vertical" @finish="handleSearch">
        <div class="filter-grid">
          <a-form-item label="用户 ID">
            <a-input v-model:value="filters.id" allow-clear placeholder="Snowflake ID" />
          </a-form-item>
          <a-form-item label="用户账号">
            <a-input v-model:value="filters.userAccount" allow-clear placeholder="账号关键词" />
          </a-form-item>
          <a-form-item label="用户昵称">
            <a-input v-model:value="filters.userName" allow-clear placeholder="昵称关键词" />
          </a-form-item>
          <a-form-item label="用户角色">
            <a-select v-model:value="filters.userRole" allow-clear placeholder="全部角色">
              <a-select-option value="user">普通用户</a-select-option>
              <a-select-option value="admin">管理员</a-select-option>
              <a-select-option value="ban">已封禁</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="个人简介">
            <a-input v-model:value="filters.userProfile" allow-clear placeholder="简介关键词" />
          </a-form-item>
        </div>
        <div class="filter-actions">
          <a-button @click="resetFilters">重置</a-button>
          <a-button type="primary" html-type="submit">查询用户</a-button>
        </div>
      </a-form>
    </section>

    <section class="table-card" aria-label="用户列表">
      <a-table
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :pagination="false"
        :row-key="(record: API.UserVO) => record.id || ''"
        :scroll="{ x: 1240 }"
      >
        <template #emptyText>
          <a-empty description="没有找到符合条件的用户" />
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'user'">
            <button class="user-cell" type="button" @click="openDetail(record)">
              <a-avatar :size="44" :src="record.userAvatar">
                {{ getUserDisplayName(record).slice(0, 1).toUpperCase() }}
              </a-avatar>
              <span>
                <strong>
                  {{ getUserDisplayName(record) }}
                  <em v-if="record.id === loginUserStore.loginUser?.id">当前账号</em>
                </strong>
                <small>@{{ record.userAccount || 'unknown' }}</small>
              </span>
            </button>
          </template>
          <template v-else-if="column.key === 'userRole'">
            <a-tag :color="getRoleMeta(record.userRole).color">
              {{ getRoleMeta(record.userRole).label }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'userProfile'">
            <p class="profile-cell">{{ record.userProfile || '暂未填写个人简介' }}</p>
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ formatDateTime(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <div class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">详情</a-button>
              <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
              <a-tooltip
                :title="record.id === loginUserStore.loginUser?.id ? '不能删除当前登录账号' : ''"
              >
                <span>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    :disabled="record.id === loginUserStore.loginUser?.id"
                    @click="confirmDelete(record)"
                  >
                    删除
                  </a-button>
                </span>
              </a-tooltip>
            </div>
          </template>
        </template>
      </a-table>

      <div class="admin-pagination">
        <a-pagination
          :current="pageNum"
          :page-size="pageSize"
          :total="total"
          :page-size-options="['10', '20', '50', '100', '200']"
          show-size-changer
          show-quick-jumper
          :show-total="(value: number) => `共 ${value} 条`"
          @change="handlePageChange"
        />
      </div>
    </section>

    <a-drawer v-model:open="detailVisible" title="用户详情" width="min(520px, 94vw)">
      <template v-if="detailUser">
        <div class="detail-identity">
          <a-avatar :size="76" :src="detailUser.userAvatar">
            {{ getUserDisplayName(detailUser).slice(0, 1).toUpperCase() }}
          </a-avatar>
          <div>
            <span>{{ getRoleMeta(detailUser.userRole).label }}</span>
            <h2>{{ getUserDisplayName(detailUser) }}</h2>
            <p>@{{ detailUser.userAccount || 'unknown' }}</p>
          </div>
        </div>
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="用户 ID">{{ detailUser.id || '—' }}</a-descriptions-item>
          <a-descriptions-item label="用户角色">
            <a-tag :color="getRoleMeta(detailUser.userRole).color">
              {{ getRoleMeta(detailUser.userRole).label }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ formatDateTime(detailUser.createTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="个人简介">
            <p class="detail-profile">{{ detailUser.userProfile || '暂未填写个人简介' }}</p>
          </a-descriptions-item>
          <a-descriptions-item label="头像地址">
            <a
              v-if="detailUser.userAvatar"
              class="avatar-link"
              :href="detailUser.userAvatar"
              target="_blank"
              rel="noreferrer"
            >
              查看原图
            </a>
            <span v-else>—</span>
          </a-descriptions-item>
        </a-descriptions>
        <div class="drawer-actions">
          <a-button type="primary" @click="editDetailUser">编辑用户</a-button>
        </div>
      </template>
    </a-drawer>

    <a-modal
      v-model:open="editorVisible"
      :title="editorMode === 'create' ? '新增用户' : '编辑用户'"
      :confirm-loading="submitting"
      :ok-text="editorMode === 'create' ? '创建用户' : '保存修改'"
      cancel-text="取消"
      width="min(620px, 94vw)"
      centered
      @ok="submitEditor"
    >
      <a-alert
        v-if="editorMode === 'create'"
        class="password-notice"
        type="info"
        show-icon
        message="新用户的初始密码为 12345678，请提醒用户登录后妥善保管账号。"
      />
      <a-form ref="userFormRef" :model="editorForm" :rules="editorRules" layout="vertical">
        <div class="editor-grid">
          <a-form-item label="用户账号" name="userAccount">
            <a-input
              v-model:value="editorForm.userAccount"
              :disabled="editorMode === 'edit'"
              allow-clear
              placeholder="至少 4 位，仅创建时可填写"
            />
          </a-form-item>
          <a-form-item label="用户昵称" name="userName">
            <a-input
              v-model:value="editorForm.userName"
              allow-clear
              placeholder="用户对外展示名称"
            />
          </a-form-item>
          <a-form-item label="用户角色" name="userRole">
            <a-select
              v-model:value="editorForm.userRole"
              :disabled="isEditingSelf"
              placeholder="请选择角色"
            >
              <a-select-option value="user">普通用户</a-select-option>
              <a-select-option value="admin">管理员</a-select-option>
            </a-select>
            <small v-if="isEditingSelf" class="field-help">不能修改当前登录账号的管理员角色</small>
          </a-form-item>
          <a-form-item label="头像地址" name="userAvatar">
            <a-input
              v-model:value="editorForm.userAvatar"
              allow-clear
              placeholder="https://example.com/avatar.png"
            />
          </a-form-item>
        </div>
        <a-form-item label="个人简介" name="userProfile">
          <a-textarea
            v-model:value="editorForm.userProfile"
            :auto-size="{ minRows: 3, maxRows: 6 }"
            :maxlength="500"
            show-count
            placeholder="介绍该用户的身份或职责"
          />
        </a-form-item>
        <div v-if="editorForm.userAvatar" class="avatar-preview">
          <a-avatar :size="48" :src="editorForm.userAvatar">
            {{ (editorForm.userName || editorForm.userAccount || '用').slice(0, 1) }}
          </a-avatar>
          <span>头像预览</span>
        </div>
      </a-form>
    </a-modal>
  </main>
</template>

<style scoped>
.admin-page {
  width: min(100% - 32px, var(--app-content-width));
  margin: 48px auto 90px;
}

.admin-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 26px;
}

.admin-heading > div:first-child > span {
  color: var(--app-primary-deep);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.18em;
}

.admin-heading h1 {
  margin: 5px 0;
  color: var(--app-ink);
  font-size: 36px;
  letter-spacing: -0.04em;
}

.admin-heading p {
  margin: 0;
  color: var(--app-muted);
}

.admin-heading__actions {
  display: flex;
  align-items: stretch;
  flex: 0 0 auto;
  gap: 10px;
}

.admin-heading__actions :deep(.ant-btn-primary) {
  height: auto;
  padding-inline: 22px;
  background: var(--app-primary);
}

.admin-heading__stat {
  display: grid;
  padding: 10px 18px;
  grid-template-columns: auto auto;
  align-items: baseline;
  column-gap: 7px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--app-border);
  border-radius: 14px;
}

.admin-heading__stat strong {
  color: var(--app-primary-deep);
  font-size: 26px;
}

.admin-heading__stat span,
.admin-heading__stat small {
  color: var(--app-muted);
  font-size: 12px;
}

.admin-heading__stat small {
  grid-column: 1 / -1;
}

.filter-card,
.table-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.96);
  border-radius: 20px;
  box-shadow: var(--app-shadow-soft);
}

.filter-card {
  margin-bottom: 18px;
  padding: 24px 26px 18px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  column-gap: 18px;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 9px;
}

.filter-actions :deep(.ant-btn-primary) {
  background: var(--app-primary);
}

.table-card {
  overflow: hidden;
  padding: 6px 6px 20px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  padding: 0;
  text-align: left;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.user-cell > span:last-child {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.user-cell strong {
  display: flex;
  align-items: center;
  min-width: 0;
  overflow: hidden;
  color: var(--app-ink);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-cell strong em {
  flex: 0 0 auto;
  margin-left: 7px;
  padding: 2px 6px;
  color: var(--app-primary-deep);
  font-size: 10px;
  font-style: normal;
  background: #e5f8f2;
  border-radius: 999px;
}

.user-cell small {
  color: var(--app-muted);
}

.profile-cell {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: #536174;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.table-actions {
  display: flex;
  white-space: nowrap;
}

.admin-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 20px 16px 0;
}

.detail-identity {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 24px;
  padding: 22px;
  color: #fff;
  background: linear-gradient(135deg, #087c71, #1769e0);
  border-radius: 18px;
}

.detail-identity :deep(.ant-avatar) {
  flex: 0 0 auto;
  border: 3px solid rgba(255, 255, 255, 0.74);
}

.detail-identity span {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  opacity: 0.8;
}

.detail-identity h2 {
  margin: 3px 0 1px;
  color: #fff;
  font-size: 24px;
}

.detail-identity p,
.detail-profile {
  margin: 0;
}

.detail-identity p {
  opacity: 0.78;
}

.detail-profile {
  line-height: 1.7;
  white-space: pre-wrap;
}

.avatar-link {
  color: var(--app-blue);
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 22px;
}

.password-notice {
  margin-bottom: 20px;
}

.editor-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 18px;
}

.field-help {
  display: block;
  margin-top: 5px;
  color: var(--app-muted);
}

.avatar-preview {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: -4px;
  padding: 10px 12px;
  color: var(--app-muted);
  font-size: 12px;
  background: #f5f8fa;
  border-radius: 12px;
}

@media (max-width: 1000px) {
  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .admin-page {
    margin-top: 32px;
  }

  .admin-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .admin-heading__actions {
    width: 100%;
  }

  .admin-heading__stat {
    flex: 1;
  }

  .filter-grid,
  .editor-grid {
    grid-template-columns: 1fr;
  }

  .filter-card {
    padding-inline: 18px;
  }

  .admin-pagination {
    justify-content: center;
    overflow-x: auto;
  }
}

@media (max-width: 420px) {
  .admin-heading__actions {
    flex-direction: column;
  }

  .admin-heading__actions :deep(.ant-btn-primary) {
    min-height: 44px;
  }
}
</style>
