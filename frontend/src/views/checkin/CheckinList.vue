<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'

import { deleteCheckinApi, getCheckinListApi } from '@/api/checkin'
import { getGoalListApi } from '@/api/goal'

const loading = ref(false)
const goalList = ref([])
const checkinList = ref([])
const queryForm = reactive({
  goalId: ''
})

const loadGoalList = async () => {
  const data = await getGoalListApi()
  goalList.value = Array.isArray(data) ? data : []
}

const loadCheckins = async () => {
  loading.value = true
  try {
    const data = await getCheckinListApi({
      goalId: queryForm.goalId || undefined
    })
    checkinList.value = Array.isArray(data) ? data : []
  } finally {
    loading.value = false
  }
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
      <template #header>
        <span>打卡记录</span>
      </template>

      <div class="toolbar">
        <el-select v-model="queryForm.goalId" clearable placeholder="按目标筛选" style="width: 240px">
          <el-option v-for="item in goalList" :key="item.id" :label="item.title" :value="item.id" />
        </el-select>
        <el-button type="primary" @click="loadCheckins">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="checkinList" stripe>
        <el-table-column prop="checkinDate" label="打卡日期" min-width="120" />
        <el-table-column prop="goalTitle" label="目标名称" min-width="180" />
        <el-table-column prop="studyDuration" label="学习时长(分钟)" min-width="130" />
        <el-table-column prop="content" label="学习内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
