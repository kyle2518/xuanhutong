import { defineStore } from 'pinia'
import { ref } from 'vue'
import { herbApi } from '@/api/herbs'

export const useHerbStore = defineStore('herbs', () => {
  const tooltips = ref<Map<number, string>>(new Map())

  async function fetchTooltip(id: number): Promise<string> {
    if (tooltips.value.has(id)) return tooltips.value.get(id)!
    try {
      const res = await herbApi.getTooltip(id)
      const text = res.data.data
      tooltips.value.set(id, text)
      return text
    } catch {
      return ''
    }
  }

  async function searchHerbs(keyword: string) {
    const res = await herbApi.search({ keyword, size: 20 })
    return res.data.data?.records || []
  }

  return { tooltips, fetchTooltip, searchHerbs }
})
