<template>
  <div class="global-footer">
    <span>© 2026 AI ZeroCode Studio. Crafted for fast product delivery.</span>
  </div>
</template>

<style scoped>
.global-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  width: min(100%, var(--app-content-width));
  height: 100%;
  margin: 0 auto;
  color: var(--app-text-secondary);
  font-size: 0.875rem;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.52), rgba(255, 255, 255, 0.9));
  border: 1px solid var(--app-border);
  border-radius: 18px 18px 0 0;
  backdrop-filter: blur(12px);
}

@media (max-width: 767px) {
  .global-footer {
    padding-inline: 16px;
    text-align: center;
    font-size: 0.75rem;
  }
}
</style>
