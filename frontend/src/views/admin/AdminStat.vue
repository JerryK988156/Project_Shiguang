<script setup>
import { onMounted, ref } from 'vue'

import { getAdminOverviewApi, getAdminUserListApi } from '@/api/admin'

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
        <el-table-column prop="username" label="账号" min-width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="140" />
        <el-table-column prop="role" label="角色" min-width="120" />
        <el-table-column prop="status" label="状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
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
</style>
