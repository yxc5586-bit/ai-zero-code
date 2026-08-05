<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    description: string
    total: number
    totalLabel: string
    note?: string
  }>(),
  { note: '' },
)
</script>

<template>
  <header class="admin-heading">
    <div>
      <span>ADMIN CONSOLE</span>
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
    </div>
    <div class="admin-heading__actions">
      <div class="admin-heading__stat" aria-live="polite">
        <strong>{{ total }}</strong>
        <span>{{ totalLabel }}</span>
        <small v-if="note">{{ note }}</small>
      </div>
      <slot name="actions"></slot>
    </div>
  </header>
</template>

<style scoped>
.admin-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 22px;
}

.admin-heading > div:first-child > span {
  color: var(--app-primary-deep);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.18em;
}

.admin-heading h1 {
  margin: 4px 0;
  color: var(--app-ink);
  font-size: 32px;
  letter-spacing: -0.04em;
}

.admin-heading p {
  margin: 0;
  color: var(--app-muted);
}

.admin-heading__actions {
  display: flex;
  align-items: stretch;
  flex: 0 0 auto;
  gap: 10px;
}

.admin-heading__actions :deep(.ant-btn-primary) {
  height: auto;
  min-height: 42px;
  padding-inline: 20px;
  background: var(--app-primary);
}

.admin-heading__stat {
  display: grid;
  padding: 8px 15px;
  grid-template-columns: auto auto;
  align-items: baseline;
  column-gap: 6px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--app-border);
  border-radius: 12px;
}

.admin-heading__stat strong {
  color: var(--app-primary-deep);
  font-size: 23px;
}

.admin-heading__stat span,
.admin-heading__stat small {
  color: var(--app-muted);
  font-size: 11px;
}

.admin-heading__stat small {
  grid-column: 1 / -1;
}

@media (max-width: 680px) {
  .admin-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .admin-heading__actions {
    width: 100%;
  }

  .admin-heading__stat {
    flex: 1;
  }
}

@media (max-width: 420px) {
  .admin-heading__actions {
    flex-direction: column;
  }
}
</style>
