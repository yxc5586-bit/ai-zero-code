<template>
  <footer class="global-footer">
    <span>© 2026 AI ZeroCode Studio</span>
    <span>让每一个想法，都有机会成为网站。</span>
  </footer>
</template>

<style scoped>
.global-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: min(100%, var(--app-content-width));
  margin: 0 auto;
  padding: 18px 24px;
  color: var(--app-muted);
  font-size: 13px;
  border-top: 1px solid var(--app-border);
}

@media (max-width: 575px) {
  .global-footer {
    flex-direction: column;
    gap: 4px;
    text-align: center;
  }
}
</style>
