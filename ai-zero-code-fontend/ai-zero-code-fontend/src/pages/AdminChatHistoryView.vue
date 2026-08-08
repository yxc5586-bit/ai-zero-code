<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'

import { listAllChatHistoryByPageForAdmin } from '@/api/chatHistoryController'
import AdminPageHeader from '@/components/AdminPageHeader.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { cleanQueryParams, getApiErrorMessage, toSafePageNumber } from '@/utils/api'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const records = ref<API.ChatHistory[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const detailRecord = ref<API.ChatHistory | null>(null)
const filters = reactive<API.ChatHistoryQueryRequest>({
  id: undefined,
  message: '',
  messageType: '',
  appId: undefined,
  userId: undefined,
})

const columns = [
  { title: '消息内容', key: 'message', width: 360, fixed: 'left' },
  { title: '消息 ID', dataIndex: 'id', key: 'id', width: 190 },
  { title: '消息类型', dataIndex: 'messageType', key: 'messageType', width: 110 },
  { title: '应用 ID', dataIndex: 'appId', key: 'appId', width: 190 },
  { title: '用户 ID', dataIndex: 'userId', key: 'userId', width: 190 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 },
  { title: '操作', key: 'action', width: 90, fixed: 'right' },
]

const cleanFilters = computed(() => cleanQueryParams(filters))

const formatMessageType = (messageType?: string) => (messageType === 'ai' ? 'AI' : '用户')

const fetchChatHistory = async () => {
  loading.value = true
  try {
    const response = await listAllChatHistoryByPageForAdmin({
      ...cleanFilters.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      sortField: 'createTime',
      sortOrder: 'descend',
    })
    if (response.data.code !== 0) {
      throw new Error(response.data.message || '对话列表加载失败')
    }
    records.value = response.data.data?.records ?? []
    total.value = toSafePageNumber(response.data.data?.totalRow)
  } catch (error) {
    message.error(getApiErrorMessage(error, '对话列表加载失败'))
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  void fetchChatHistory()
}

const resetFilters = () => {
  Object.assign(filters, {
    id: undefined,
    message: '',
    messageType: '',
    appId: undefined,
    userId: undefined,
  })
  pageNum.value = 1
  void fetchChatHistory()
}

const openDetail = (record: API.ChatHistory) => {
  detailRecord.value = record
  detailVisible.value = true
}

onMounted(fetchChatHistory)
</script>

<template>
  <main class="admin-page page-enter">
    <AdminPageHeader
      title="对话管理"
      description="查询和查看平台内应用的全部用户与 AI 对话记录。"
      :total="total"
      total-label="条消息"
    />

    <section class="filter-card">
      <a-form layout="vertical" @finish="handleSearch">
        <div class="filter-grid">
          <a-form-item label="消息 ID">
            <a-input v-model:value="filters.id" allow-clear placeholder="Snowflake ID" />
          </a-form-item>
          <a-form-item label="消息内容">
            <a-input v-model:value="filters.message" allow-clear placeholder="内容关键词" />
          </a-form-item>
          <a-form-item label="消息类型">
            <a-select v-model:value="filters.messageType" allow-clear placeholder="全部类型">
              <a-select-option value="user">用户消息</a-select-option>
              <a-select-option value="ai">AI 消息</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="应用 ID">
            <a-input v-model:value="filters.appId" allow-clear placeholder="应用 ID" />
          </a-form-item>
          <a-form-item label="用户 ID">
            <a-input v-model:value="filters.userId" allow-clear placeholder="用户 ID" />
          </a-form-item>
        </div>
        <div class="filter-actions">
          <a-button @click="resetFilters">重置</a-button>
          <a-button type="primary" @click="handleSearch">查询对话</a-button>
        </div>
      </a-form>
    </section>

    <section class="table-card">
      <a-table
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :pagination="false"
        :row-key="(record: API.ChatHistory) => record.id || `${record.appId}-${record.createTime}`"
        :scroll="{ x: 1480 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'message'">
            <button class="message-cell" type="button" @click="openDetail(record)">
              <span class="message-cell__icon" :class="`message-cell__icon--${record.messageType}`">
                {{ record.messageType === 'ai' ? 'AI' : '用' }}
              </span>
              <span>
                <strong>{{ formatMessageType(record.messageType) }}消息</strong>
                <small>{{ record.message || '暂无消息内容' }}</small>
              </span>
            </button>
          </template>
          <template v-else-if="column.key === 'messageType'">
            <a-tag :color="record.messageType === 'ai' ? 'blue' : 'green'">
              {{ formatMessageType(record.messageType) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ formatDateTime(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'updateTime'">
            {{ formatDateTime(record.updateTime) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record)">详情</a-button>
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
          @change="fetchChatHistory"
          @show-size-change="fetchChatHistory"
        />
      </div>
    </section>

    <a-modal v-model:open="detailVisible" title="对话详情" :footer="null" width="780px">
      <a-descriptions v-if="detailRecord" bordered :column="2" size="small">
        <a-descriptions-item label="消息 ID" :span="2">{{ detailRecord.id }}</a-descriptions-item>
        <a-descriptions-item label="消息类型">
          <a-tag :color="detailRecord.messageType === 'ai' ? 'blue' : 'green'">
            {{ formatMessageType(detailRecord.messageType) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="应用 ID">{{ detailRecord.appId }}</a-descriptions-item>
        <a-descriptions-item label="用户 ID">{{ detailRecord.userId }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">
          {{ formatDateTime(detailRecord.createTime) }}
        </a-descriptions-item>
        <a-descriptions-item label="更新时间" :span="2">
          {{ formatDateTime(detailRecord.updateTime) }}
        </a-descriptions-item>
      </a-descriptions>
      <div v-if="detailRecord" class="message-detail">
        <strong>消息内容</strong>
        <div class="message-detail__content">
          <MarkdownRenderer
            v-if="detailRecord.messageType === 'ai'"
            :content="detailRecord.message || ''"
          />
          <p v-else>{{ detailRecord.message || '暂无消息内容' }}</p>
        </div>
      </div>
    </a-modal>
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

.message-cell {
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

.message-cell__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
  color: #087c71;
  font-size: 12px;
  font-weight: 800;
  background: #e6f8f2;
  border-radius: 10px;
}

.message-cell__icon--ai {
  color: #2563a8;
  background: #eaf3ff;
}

.message-cell > span:last-child {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.message-cell strong {
  color: var(--app-ink);
}

.message-cell small {
  max-width: 290px;
  overflow: hidden;
  color: var(--app-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 14px 0;
}

.message-detail {
  margin-top: 20px;
}

.message-detail > strong {
  display: block;
  margin-bottom: 9px;
  color: var(--app-ink);
}

.message-detail__content {
  max-height: 50vh;
  overflow: auto;
  padding: 14px 16px;
  background: #f6f8fa;
  border: 1px solid var(--app-border);
  border-radius: 10px;
}

.message-detail__content p {
  margin: 0;
  line-height: 1.75;
  white-space: pre-wrap;
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
