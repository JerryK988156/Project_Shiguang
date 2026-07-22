<script setup>
import { onMounted, ref } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'

import { useUserStore } from '@/stores/user'
import { getAdminOverviewApi, getAdminUserListApi, updateUserStatusApi } from '@/api/admin'

const userStore = useUserStore()
const loading = ref(false)
const overview = ref({})
const userList = ref([])

const loadAdminData = async () => {
  loading.value = true
  try {
    const [overviewData, usersData] = await Promise.all([
      getAdminOverviewApi(),
      getAdminUserListApi()
    ])
    overview.value = overviewData || {}
    userList.value = Array.isArray(usersData) ? usersData : []
  } finally {
    loading.value = false
  }
}

const handleDisable = async (row) => {
  try {
    await ElMessageBox.confirm(`确认禁用用户"${row.nickname || row.username}"吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await updateUserStatusApi(row.id, 0)
    ElMessage.success('已禁用')
    await loadAdminData()
  } catch (error) {
    // 错误已由 axios 拦截器提示
  }
}

const handleEnable = async (row) => {
  try {
    await ElMessageBox.confirm(`确认解禁用户"${row.nickname || row.username}"吗？`, '提示', { type: 'info' })
  } catch {
    return
  }
  try {
    await updateUserStatusApi(row.id, 1)
    ElMessage.success('已解禁')
    await loadAdminData()
  } catch (error) {
    // 错误已由 axios 拦截器提示
  }
}

const roleLabel = (row) => {
  if (row.role !== 'admin') return '普通用户'
  return row.id === 1 ? '超级管理员' : '管理员'
}

const roleTagType = (row) => {
  if (row.role !== 'admin') return 'primary'
  return row.id === 1 ? 'danger' : 'warning'
}

onMounted(loadAdminData)
</script>

<template>
  <div class="page-container admin-page" v-loading="loading">
    <div class="info-grid">
      <el-card>
        <div class="info-label">用户总数</div>
        <div class="info-value">{{ overview.userCount || 0 }}</div>
      </el-card>
      <el-card>
        <div class="info-label">目标总数</div>
        <div class="info-value">{{ overview.goalCount || 0 }}</div>
      </el-card>
      <el-card>
        <div class="info-label">打卡总数</div>
        <div class="info-value">{{ overview.checkinCount || 0 }}</div>
      </el-card>
      <el-card>
        <div class="info-label">本周活跃用户</div>
        <div class="info-value">{{ overview.activeUserCount7d || 0 }}</div>
      </el-card>
    </div>

    <el-card>
      <template #header>
        <span>用户列表</span>
      </template>
      <el-table :data="userList" stripe>
        <el-table-column label="头像" width="70">
          <template #default="{ row }">
            <el-avatar :size="36" :src="row.avatar">
              {{ (row.nickname || row.username || '?').charAt(0) }}
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column label="身份" min-width="120">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row)">
              {{ roleLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.canModify">
              <el-button
                v-if="row.status === 1"
                size="small"
                type="danger"
                plain
                @click="handleDisable(row)"
              >
                禁用
              </el-button>
              <el-button
                v-else
                size="small"
                type="success"
                @click="handleEnable(row)"
              >
                解禁
              </el-button>
            </template>
            <span v-else-if="row.isSelf" class="text-muted">当前账号</span>
            <span v-else class="text-muted">无操作权限</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  padding: 24px;
  max-width: 1400px;
}

.admin-page {
  display: grid;
  gap: 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.info-label {
  margin-bottom: 12px;
  color: #6b7280;
}

.info-value {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

.text-muted {
  color: #9ca3af;
  font-size: 13px;
}
</style>
