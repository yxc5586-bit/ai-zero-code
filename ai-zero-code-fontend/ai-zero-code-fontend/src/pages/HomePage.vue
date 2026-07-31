<script setup lang="ts">

import { healthController } from '@/api/healthController.ts'

healthController().then((res) => {
  console.log(res)
})

</script>

<template>
  <main>

  </main>
</template>



