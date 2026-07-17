<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'

import { deleteCheckinApi, getCheckinListApi } from '@/api/checkin'
import { getGoalListApi } from '@/api/goal'

const loading = ref(false)
const goalList = ref([])
const checkinList = ref([])
const queryForm = reactive({ goalId: '' })

const detailVisible = ref(false)
const detailRow = ref(null)

const loadGoalList = async () => {
  const data = await getGoalListApi()
  goalList.value = Array.isArray(data) ? data : []
}

const loadCheckins = async () => {
  loading.value = true
  try {
    const data = await getCheckinListApi({ goalId: queryForm.goalId || undefined })
    checkinList.value = Array.isArray(data) ? data : []
  } finally {
    loading.value = false
  }
}

const handleRowClick = (row) => {
  detailRow.value = row
  detailVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除这条打卡记录吗？', '提示', { type: 'warning' })
  await deleteCheckinApi(row.id)
  ElMessage.success('删除成功')
  loadCheckins()
}

onMounted(async () => {
  await loadGoalList()
  await loadCheckins()
})
</script>

<template>
  <div class="page-container">
    <el-card>
      <template #header><span>打卡记录</span></template>

      <div class="toolbar">
        <el-select v-model="queryForm.goalId" clearable placeholder="按目标筛选" style="width: 240px">
          <el-option v-for="item in goalList" :key="item.id" :label="item.title" :value="item.id" />
        </el-select>
        <el-button type="primary" @click="loadCheckins">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="checkinList" stripe @row-click="handleRowClick" style="cursor: pointer">
        <el-table-column prop="checkinDate" label="打卡日期" min-width="120" />
        <el-table-column prop="goalTitle" label="目标名称" min-width="180" />
        <el-table-column prop="studyDuration" label="学习时长(分钟)" min-width="130" />
        <el-table-column prop="content" label="学习内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" title="打卡详情" width="520px">
      <template v-if="detailRow">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="打卡日期">{{ detailRow.checkinDate }}</el-descriptions-item>
          <el-descriptions-item label="目标名称">{{ detailRow.goalTitle }}</el-descriptions-item>
          <el-descriptions-item label="学习时长">{{ detailRow.studyDuration }} 分钟</el-descriptions-item>
          <el-descriptions-item label="学习内容">{{ detailRow.content || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注 / 心得">{{ detailRow.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>
