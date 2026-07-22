<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'

import { deleteGoalApi, getGoalListApi, updateGoalApi, updateGoalStatusApi } from '@/api/goal'

const router = useRouter()
const loading = ref(false)
const goalList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const keyword = ref('')
let debounceTimer = null
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
      status: queryForm.status || undefined,
      keyword: keyword.value || undefined
    })
    goalList.value = Array.isArray(data) ? data : []
    currentPage.value = 1
  } finally {
    loading.value = false
  }
}

const onKeywordInput = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    loadGoals()
  }, 300)
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

const drawerVisible = ref(false)
const drawerGoal = reactive({
  id: '',
  title: '',
  description: '',
  startDate: '',
  endDate: '',
  targetDays: 0,
  status: '进行中'
})
const drawerTags = ref([])
const drawerTagInput = ref('')
const drawerLoading = ref(false)

const openDrawer = (row) => {
  Object.assign(drawerGoal, {
    id: row.id,
    title: row.title,
    description: row.description || '',
    startDate: row.startDate,
    endDate: row.endDate,
    targetDays: row.targetDays,
    status: row.status
  })
  drawerTags.value = [...(row.tags || [])]
  drawerVisible.value = true
}

const drawerAddTag = () => {
  const val = drawerTagInput.value.trim()
  if (!val) return
  if (drawerTags.value.includes(val)) { ElMessage.warning('标签已存在'); return }
  drawerTags.value.push(val)
  drawerTagInput.value = ''
}

const drawerRemoveTag = (tag) => {
  drawerTags.value = drawerTags.value.filter(t => t !== tag)
}

const drawerSave = async () => {
  if (!drawerGoal.title || !drawerGoal.startDate) {
    ElMessage.warning('请填写目标标题和日期')
    return
  }
  drawerLoading.value = true
  try {
    await updateGoalApi({ ...drawerGoal, tags: drawerTags.value })
    ElMessage.success('保存成功')
    drawerVisible.value = false
    loadGoals()
  } finally {
    drawerLoading.value = false
  }
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
        <el-input
          v-model="keyword"
          placeholder="搜索目标标题或标签..."
          clearable
          style="width: 260px"
          @input="onKeywordInput"
          @clear="loadGoals"
        />
        <el-select v-model="queryForm.status" clearable placeholder="按状态筛选" style="width: 180px" @change="loadGoals" @clear="loadGoals">
          <el-option label="进行中" value="进行中" />
          <el-option label="已完成" value="已完成" />
          <el-option label="已放弃" value="已放弃" />
        </el-select>
        <el-button v-if="queryForm.status" link type="primary" @click="queryForm.status = ''; loadGoals()">显示全部</el-button>
      </div>

      <template v-if="loading">
        <el-skeleton animated :rows="8" :throttle="0" style="padding: 24px" />
      </template>
      <template v-else>
        <el-empty v-if="goalList.length === 0" description="还没有学习目标">
          <el-button type="primary" @click="router.push('/goal/edit')">创建第一个目标</el-button>
        </el-empty>
        <template v-else>
          <el-table :data="pagedGoalList" stripe @row-click="openDrawer" style="cursor: pointer">
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
        </template>
      </template>
    </el-card>

    <el-drawer v-model="drawerVisible" title="目标详情" size="480px" direction="rtl">
      <el-form label-width="80px" v-loading="drawerLoading">
        <el-form-item label="目标标题">
          <el-input v-model="drawerGoal.title" placeholder="请输入目标标题" />
        </el-form-item>
        <el-form-item label="目标说明">
          <el-input v-model="drawerGoal.description" type="textarea" :rows="3" placeholder="请输入目标说明" />
        </el-form-item>
        <el-form-item label="标签">
          <div style="display:flex;flex-wrap:wrap;gap:4px;align-items:center">
            <el-tag v-for="tag in drawerTags" :key="tag" closable @close="drawerRemoveTag(tag)" size="small" style="margin-right:4px;margin-bottom:4px">{{ tag }}</el-tag>
            <el-input v-if="drawerTags.length < 10" v-model="drawerTagInput" placeholder="新标签" size="small" style="width:100px" @keyup.enter="drawerAddTag" />
          </div>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="drawerGoal.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="drawerGoal.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="drawerGoal.status" style="width:100%">
            <el-option label="进行中" value="进行中" />
            <el-option label="已完成" value="已完成" />
            <el-option label="已放弃" value="已放弃" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" @click="drawerSave" :loading="drawerLoading">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.page-container {
  padding: 24px;
  max-width: 1400px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.text-muted { color: #c0c4cc; }
</style>
