<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { useRouter } from 'vue-router'

import { deleteAppByAdmin, listAppVoByPageByAdmin, updateAppByAdmin } from '@/api/appController'
import AdminPageHeader from '@/components/AdminPageHeader.vue'
import AppCover from '@/components/AppCover.vue'
import AppDetailDrawer from '@/components/AppDetailDrawer.vue'
import { cleanQueryParams, getApiErrorMessage, toSafePageNumber } from '@/utils/api'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const records = ref<API.AppVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const detailApp = ref<API.AppVO | null>(null)
const filters = reactive<API.AppQueryRequest>({
  id: undefined,
  appName: '',
  cover: '',
  initPrompt: '',
  codeGenType: '',
  deployKey: '',
  priority: undefined,
  userId: undefined,
})

const columns = [
  { title: '应用', key: 'app', width: 260, fixed: 'left' },
  { title: '应用 ID', dataIndex: 'id', key: 'id', width: 190 },
  { title: '生成类型', dataIndex: 'codeGenType', key: 'codeGenType', width: 110 },
  { title: '创建者', key: 'creator', width: 150 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 90 },
  { title: '部署状态', key: 'deploy', width: 110 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 250, fixed: 'right' },
]

const cleanFilters = computed(() => cleanQueryParams(filters))

const fetchApps = async () => {
  loading.value = true
  try {
    const response = await listAppVoByPageByAdmin({
      ...cleanFilters.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      sortField: 'createTime',
      sortOrder: 'descend',
    })
    if (response.data.code !== 0) throw new Error(response.data.message || '应用列表加载失败')
    records.value = response.data.data?.records ?? []
    total.value = toSafePageNumber(response.data.data?.totalRow)
  } catch (error) {
    message.error(getApiErrorMessage(error, '应用列表加载失败'))
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  void fetchApps()
}
const resetFilters = () => {
  Object.assign(filters, {
    id: undefined,
    appName: '',
    cover: '',
    initPrompt: '',
    codeGenType: '',
    deployKey: '',
    priority: undefined,
    userId: undefined,
  })
  pageNum.value = 1
  fetchApps()
}
const openDetail = (app: API.AppVO) => {
  detailApp.value = app
  detailVisible.value = true
}
const editApp = (app: API.AppVO) => {
  if (app.id) {
    detailVisible.value = false
    router.push({ name: 'appEdit', params: { id: app.id }, query: { from: 'admin' } })
  }
}

const confirmDelete = (app: API.AppVO) => {
  Modal.confirm({
    title: `删除应用“${app.appName || '未命名应用'}”？`,
    content: `应用 ID：${app.id}`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      if (!app.id) return
      const response = await deleteAppByAdmin({ id: app.id })
      if (response.data.code !== 0 || !response.data.data)
        throw new Error(response.data.message || '删除失败')
      message.success('应用已删除')
      if (records.value.length === 1 && pageNum.value > 1) pageNum.value -= 1
      await fetchApps()
    },
  })
}

const featureApp = async (app: API.AppVO) => {
  if (!app.id) return

  const isFeatured = app.priority === 99
  const nextPriority = isFeatured ? 0 : 99
  const actionText = isFeatured ? '取消精选' : '设置精选'

  try {
    const response = await updateAppByAdmin({ id: app.id, priority: nextPriority })
    if (response.data.code !== 0 || !response.data.data) {
      message.error(response.data.message || `${actionText}失败`)
      return
    }
    message.success(isFeatured ? '已取消精选' : '已设置为精选应用')
    await fetchApps()
  } catch (error) {
    message.error(getApiErrorMessage(error, `${actionText}失败`))
  }
}

onMounted(fetchApps)
</script>

<template>
  <main class="admin-page page-enter">
    <AdminPageHeader
      title="应用管理"
      description="查询、审核和维护平台内的全部应用。"
      :total="total"
      total-label="个应用"
    />

    <section class="filter-card">
      <a-form layout="vertical" @finish="handleSearch">
        <div class="filter-grid">
          <a-form-item label="应用 ID"
            ><a-input v-model:value="filters.id" allow-clear placeholder="Snowflake ID"
          /></a-form-item>
          <a-form-item label="应用名称"
            ><a-input v-model:value="filters.appName" allow-clear placeholder="名称关键词"
          /></a-form-item>
          <a-form-item label="创建用户 ID"
            ><a-input v-model:value="filters.userId" allow-clear placeholder="用户 ID"
          /></a-form-item>
          <a-form-item label="生成类型"
            ><a-select v-model:value="filters.codeGenType" allow-clear placeholder="全部类型"
              ><a-select-option value="multi_file">多文件</a-select-option
              ><a-select-option value="html">HTML</a-select-option></a-select
            ></a-form-item
          >
          <a-form-item label="初始提示词"
            ><a-input v-model:value="filters.initPrompt" allow-clear placeholder="提示词关键词"
          /></a-form-item>
          <a-form-item label="封面地址"
            ><a-input v-model:value="filters.cover" allow-clear placeholder="封面 URL"
          /></a-form-item>
          <a-form-item label="部署标识"
            ><a-input v-model:value="filters.deployKey" allow-clear placeholder="deployKey"
          /></a-form-item>
          <a-form-item label="优先级"
            ><a-input-number
              v-model:value="filters.priority"
              :min="0"
              :max="999"
              style="width: 100%"
              placeholder="例如 99"
          /></a-form-item>
        </div>
        <div class="filter-actions">
          <a-button @click="resetFilters">重置</a-button
          ><a-button type="primary" @click="handleSearch">查询应用</a-button>
        </div>
      </a-form>
    </section>

    <section class="table-card">
      <a-table
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :pagination="false"
        :row-key="(record: API.AppVO) => record.id || ''"
        :scroll="{ x: 1450 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'app'">
            <button class="app-cell" type="button" @click="openDetail(record)">
              <span class="app-cell__cover"><AppCover :app="record" compact /></span>
              <span
                ><strong>{{ record.appName || '未命名应用' }}</strong
                ><small>{{ record.initPrompt || '暂无初始提示词' }}</small></span
              >
            </button>
          </template>
          <template v-else-if="column.key === 'codeGenType'"
            ><a-tag color="blue">{{
              record.codeGenType === 'html' ? 'HTML' : '多文件'
            }}</a-tag></template
          >
          <template v-else-if="column.key === 'creator'"
            ><div class="creator-cell">
              <a-avatar :size="26" :src="record.createUser?.userAvatar">{{
                (record.createUser?.userName || record.createUser?.userAccount || '用').slice(0, 1)
              }}</a-avatar
              ><span>{{
                record.createUser?.userName || record.createUser?.userAccount || record.userId
              }}</span>
            </div></template
          >
          <template v-else-if="column.key === 'priority'"
            ><a-tag :color="record.priority === 99 ? 'purple' : 'default'"
              >{{ record.priority ?? 0 }}{{ record.priority === 99 ? ' · 精选' : '' }}</a-tag
            ></template
          >
          <template v-else-if="column.key === 'deploy'"
            ><a-badge
              :status="record.deployKey ? 'success' : 'default'"
              :text="record.deployKey ? '已部署' : '未部署'"
          /></template>
          <template v-else-if="column.key === 'createTime'">{{
            formatDateTime(record.createTime)
          }}</template>
          <template v-else-if="column.key === 'action'">
            <div class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">详情</a-button
              ><a-button type="link" size="small" @click="editApp(record)">编辑</a-button
              ><a-button type="link" size="small" @click="featureApp(record)">{{
                record.priority === 99 ? '取消精选' : '设为精选'
              }}</a-button
              ><a-button type="link" size="small" danger @click="confirmDelete(record)"
                >删除</a-button
              >
            </div>
          </template>
        </template>
      </a-table>
      <div class="admin-pagination">
        <a-pagination
          v-model:current="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-size-options="['10', '20', '50', '100', '200']"
          show-size-changer
          show-quick-jumper
          :show-total="(value: number) => `共 ${value} 条`"
          @change="fetchApps"
          @show-size-change="fetchApps"
        />
      </div>
    </section>

    <AppDetailDrawer v-model:open="detailVisible" :app="detailApp" editable @edit="editApp" />
  </main>
</template>

<style scoped>
.admin-page {
  width: min(100% - 32px, var(--app-content-width));
  margin: 40px auto 76px;
}
.filter-card,
.table-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.96);
  border-radius: 15px;
  box-shadow: var(--app-shadow-soft);
}
.filter-card {
  margin-bottom: 14px;
  padding: 18px 20px 12px;
}
.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  column-gap: 15px;
}
.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.filter-actions :deep(.ant-btn-primary) {
  background: var(--app-primary);
}
.table-card {
  overflow: hidden;
  padding: 4px 4px 15px;
}
.app-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 0;
  text-align: left;
  background: transparent;
  border: 0;
  cursor: pointer;
}
.app-cell__cover {
  flex: 0 0 48px;
  width: 48px;
  height: 34px;
  overflow: hidden;
  background: #e8f3f4;
  border-radius: 6px;
}
.app-cell > span:last-child {
  display: flex;
  min-width: 0;
  flex-direction: column;
}
.app-cell strong {
  overflow: hidden;
  color: var(--app-ink);
  text-overflow: ellipsis;
  white-space: nowrap;
}
.app-cell small {
  max-width: 165px;
  overflow: hidden;
  color: var(--app-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}
.creator-cell {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}
.creator-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.table-actions {
  display: flex;
  white-space: nowrap;
}
.admin-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 14px 0;
}
@media (max-width: 1000px) {
  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 600px) {
  .admin-page {
    margin-top: 30px;
  }
  .filter-grid {
    grid-template-columns: 1fr;
  }
  .filter-card {
    padding-inline: 16px;
  }
  .admin-pagination {
    justify-content: center;
    overflow-x: auto;
  }
}
</style>
