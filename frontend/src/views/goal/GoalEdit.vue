<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { addGoalApi, getGoalDetailApi, updateGoalApi } from '@/api/goal'
import { goalTemplates } from '@/data/goalTemplates'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const goalId = route.params.id
const selectedTemplateIndex = ref(-1)
const tagInputValue = ref('')
const tags = ref([])

const form = reactive({
  id: '',
  title: '',
  description: '',
  startDate: '',
  endDate: '',
  targetDays: 0,
  status: '进行中'
})

const selectTemplate = (index) => {
  selectedTemplateIndex.value = index
  const t = goalTemplates[index]
  form.title = t.title
  form.description = t.suggestMinutes > 0
    ? `${t.description}（建议每日 ${t.suggestMinutes} 分钟）`
    : t.description
}

const handleAddTag = () => {
  const val = tagInputValue.value.trim()
  if (!val) return
  if (tags.value.includes(val)) {
    ElMessage.warning('标签已存在')
    return
  }
  tags.value.push(val)
  tagInputValue.value = ''
}

const handleRemoveTag = (tag) => {
  tags.value = tags.value.filter((t) => t !== tag)
}

const loadDetail = async () => {
  if (!goalId) {
    return
  }
  loading.value = true
  try {
    const data = await getGoalDetailApi(goalId)
    Object.assign(form, data)
    if (data.tags && Array.isArray(data.tags)) {
      tags.value = [...data.tags]
    }
  } finally {
    loading.value = false
  }
}

const disabledDate = (time) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return time.getTime() < today.getTime()
}

watch([() => form.startDate, () => form.endDate], ([start, end]) => {
  if (start && end) {
    const diff = Math.ceil((new Date(end) - new Date(start)) / (1000 * 60 * 60 * 24)) + 1
    form.targetDays = diff > 0 ? diff : 0
  }
})

const handleSubmit = async () => {
  if (!form.title || !form.startDate) {
    ElMessage.warning('请填写目标标题和开始日期')
    return
  }

  const submitData = { ...form, tags: tags.value }

  loading.value = true
  try {
    if (goalId) {
      await updateGoalApi(submitData)
      ElMessage.success('目标更新成功')
    } else {
      await addGoalApi(submitData)
      ElMessage.success('目标创建成功')
    }
    router.push('/goal/list')
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <div class="page-container">
    <el-card v-loading="loading">
      <template #header>
        <span>{{ goalId ? '编辑目标' : '新增目标' }}</span>
      </template>

      <div v-if="!goalId" class="template-section">
        <div class="template-label">选择目标模板（点击可快速填充）</div>
        <div class="template-grid">
          <el-card
            v-for="(tpl, idx) in goalTemplates"
            :key="idx"
            :class="['template-card', { 'template-card--active': selectedTemplateIndex === idx }]"
            shadow="hover"
            @click="selectTemplate(idx)"
          >
            <div class="template-title">{{ tpl.title }}</div>
            <div class="template-desc">{{ tpl.description }}</div>
            <div v-if="tpl.suggestMinutes > 0" class="template-minutes">{{ tpl.suggestMinutes }} 分钟/天</div>
          </el-card>
        </div>
      </div>

      <el-form label-width="100px">
        <el-form-item label="目标标题">
          <el-input v-model="form.title" placeholder="请输入目标标题" />
        </el-form-item>
        <el-form-item label="目标说明">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入目标说明" />
        </el-form-item>
        <el-form-item label="标签">
          <div class="tag-container">
            <el-tag
              v-for="tag in tags"
              :key="tag"
              closable
              :disable-transitions="false"
              @close="handleRemoveTag(tag)"
              style="margin-right: 8px; margin-bottom: 4px;"
            >
              {{ tag }}
            </el-tag>
            <el-input
              v-if="tags.length < 10"
              ref="tagInputRef"
              v-model="tagInputValue"
              class="tag-input-new"
              placeholder="输入标签后按回车"
              size="small"
              style="width: 150px"
              @keyup.enter="handleAddTag"
            />
          </div>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择开始日期" :disabled-date="disabledDate" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择结束日期" :disabled-date="disabledDate" />
        </el-form-item>
        <el-form-item label="计划天数">
          <el-input-number v-model="form.targetDays" :min="0" disabled />
          <span class="form-hint">根据起止日期自动计算</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 180px">
            <el-option label="进行中" value="进行中" />
            <el-option label="已完成" value="已完成" />
            <el-option label="已放弃" value="已放弃" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-space>
            <el-button type="primary" @click="handleSubmit">保存</el-button>
            <el-button @click="router.push('/goal/list')">返回</el-button>
          </el-space>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.page-container {
  padding: 24px;
  max-width: 1400px;
}
.template-section {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}
.template-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 12px;
}
.template-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.template-card {
  cursor: pointer;
  transition: border-color .3s, box-shadow .3s;
}
.template-card:hover {
  border-color: #409eff;
}
.template-card--active {
  border-color: #409eff;
  background-color: #ecf5ff;
}
.template-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}
.template-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.template-minutes {
  font-size: 12px;
  color: #67c23a;
  margin-top: 6px;
}

.tag-container {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}
.tag-input-new {
  vertical-align: middle;
}

.form-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}
</style>
