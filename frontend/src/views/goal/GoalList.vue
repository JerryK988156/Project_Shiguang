<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'

import { deleteGoalApi, getGoalListApi, updateGoalStatusApi } from '@/api/goal'

const router = useRouter()
const loading = ref(false)
const goalList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const queryForm = reactive({
  status: ''
})

const pagedGoalList = computed(() => {
  return goalList.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value)
})

const loadGoals = async () => {
  loading.value = true
  try {
    const data = await getGoalListApi({
      status: queryForm.status || undefined
    })
    goalList.value = Array.isArray(data) ? data : []
    currentPage.value = 1
  } finally {
    loading.value = false
  }
}

const handleEdit = (row) => {
  router.push(`/goal/edit/${row.id}`)
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除目标“${row.title}”吗？`, '提示', { type: 'warning' })
  await deleteGoalApi(row.id)
  ElMessage.success('删除成功')
  loadGoals()
}

const handleStatusChange = async (row, status) => {
  await updateGoalStatusApi(row.id, status)
  ElMessage.success('状态更新成功')
  loadGoals()
}

onMounted(loadGoals)
</script>

<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="page-header">
          <span>目标列表</span>
          <el-button type="primary" @click="router.push('/goal/edit')">新增目标</el-button>
        </div>
      </template>

      <div class="toolbar">
        <el-select v-model="queryForm.status" clearable placeholder="按状态筛选" style="width: 180px" @change="loadGoals" @clear="loadGoals">
          <el-option label="进行中" value="进行中" />
          <el-option label="已完成" value="已完成" />
          <el-option label="已放弃" value="已放弃" />
        </el-select>
        <el-button v-if="queryForm.status" link type="primary" @click="queryForm.status = ''; loadGoals()">显示全部</el-button>
      </div>

      <el-table v-loading="loading" :data="pagedGoalList" stripe>
        <el-table-column prop="title" label="目标标题" min-width="180" />
        <el-table-column prop="description" label="目标说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="startDate" label="开始日期" min-width="120" />
        <el-table-column prop="endDate" label="结束日期" min-width="120" />
        <el-table-column prop="targetDays" label="计划天数" min-width="100" />
        <el-table-column label="标签" min-width="150">
          <template #default="{ row }">
            <el-tag
              v-for="tag in row.tags"
              :key="tag"
              size="small"
              type="info"
              style="margin-right: 4px; margin-bottom: 2px"
            >{{ tag }}</el-tag>
            <span v-if="!row.tags || row.tags.length === 0" class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '已完成' ? 'success' : row.status === '已放弃' ? 'info' : 'warning'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-space wrap>
              <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
              <el-button link type="success" @click="handleStatusChange(row, '已完成')">标记完成</el-button>
              <el-button link type="warning" @click="handleStatusChange(row, '已放弃')">放弃</el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="goalList.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.text-muted { color: #c0c4cc; }
</style>
