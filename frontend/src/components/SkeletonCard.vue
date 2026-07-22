<script setup>
defineProps({
  rows: { type: Number, default: 4 },
  animated: { type: Boolean, default: true }
})
</script>

<template>
  <div class="skeleton-card">
    <el-skeleton :animated="animated" :rows="rows" :loading="true">
      <template #template>
        <div class="skeleton-content">
          <slot name="template" />
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<style scoped>
.skeleton-card {
  padding: 24px;
  background: #fff;
  border-radius: 16px;
  border: 1px solid #f1f5f9;
}
.skeleton-content {
  width: 100%;
}
</style>
