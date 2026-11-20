// 本地 mock 后端：只为安卓端联调/演示，不含业务逻辑，不替代真实后端。
// 用法：node server.mjs   （默认监听 0.0.0.0:8000）
// 模拟器访问：http://10.0.2.2:8000
// 预置账号：demo / 123456
import http from 'node:http'
import { randomUUID } from 'node:crypto'

const PORT = Number(process.env.PORT || 8000)
const users = new Map() // username -> { id, username, nickname, role, password }
let nextUserId = 1

function addUser(username, password, nickname) {
  const user = {
    id: nextUserId++,
    username,
    nickname: nickname || username,
    role: 'user',
    password,
  }
  users.set(username, user)
  return user
}

addUser('demo', '123456', '演示用户')

function json(res, status, payload, extraHeaders = {}) {
  const body = JSON.stringify(payload)
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'X-Trace-Id': randomUUID(),
    ...extraHeaders,
  })
  res.end(body)
}

function ok(res, data, status = 200) {
  json(res, status, { code: 0, message: 'ok', data })
}

function fail(res, status, code, message) {
  json(res, status, { code, message })
}

function readBody(req) {
  return new Promise((resolve) => {
    let raw = ''
    req.on('data', (chunk) => (raw += chunk))
    req.on('end', () => {
      try {
        resolve(raw ? JSON.parse(raw) : {})
      } catch {
        resolve({})
      }
    })
  })
}

// 只认签发过的 token：进程重启后这里清空，旧 token 立刻变成 40102，
// 跟真实后端「换 JWT_SECRET 后旧 token 全部失效」的已知坑一致，方便演示 401 统一登出。
const issuedTokens = new Set()

function currentUser(req) {
  const auth = req.headers['authorization'] || ''
  if (!auth.startsWith('Bearer ')) return null
  const token = auth.slice(7)
  if (!issuedTokens.has(token)) return null
  return users.get(token.slice('mock-token-'.length)) || null
}

// 与 whoisthe4th/backend/src/routes/home.ts 的返回结构保持一致
const homeData = {
  banners: [
    { id: 1, title: '欢迎来到西湖景区 AI 导览', image: '', color: 'linear-gradient(135deg, #1989fa, #4db8ff)' },
    { id: 2, title: '智慧导览 · 沉浸式体验', image: '', color: 'linear-gradient(135deg, #07c160, #65d67c)' },
    { id: 3, title: '语音交互 · 数字人陪伴', image: '', color: 'linear-gradient(135deg, #ff976a, #ffc77a)' },
  ],
  quickEntries: [
    { icon: 'service-o', label: 'AI 导览', path: '/interaction', color: '#1989fa' },
    { icon: 'location-o', label: '景色推荐', path: '/poi', color: '#07c160' },
    { icon: 'guide-o', label: '游玩攻略', path: '/routes', color: '#ff976a' },
    { icon: 'goods-o', label: '失物招领', path: '/lost-found', color: '#7232dd' },
  ],
  hotPois: [
    {
      id: 1,
      name: '灵隐寺',
      category: '人文古迹',
      description: '始建于东晋，江南著名古刹。',
      location: '西湖区法云弄1号',
      longitude: 120.101,
      latitude: 30.241,
      image_url: 'https://picsum.photos/seed/lingyinsi/800/600',
    },
    {
      id: 2,
      name: '九溪十八涧',
      category: '自然风光',
      description: '溪流蜿蜒，适合徒步。',
      location: '西湖区龙井村南九溪',
      longitude: 120.118,
      latitude: 30.196,
      image_url: 'https://picsum.photos/seed/jiuxi/800/600',
    },
    {
      id: 3,
      name: '雷峰塔',
      category: '人文古迹',
      description: '夕照雷峰，西湖十景之一。',
      location: '西湖区南山路15号',
      longitude: 120.148,
      latitude: 30.231,
      image_url: 'https://picsum.photos/seed/leifengta/800/600',
    },
  ],
  recommendedRoutes: [
    { id: 1, name: '西湖经典一日游', theme: '人文古迹', description: '断桥 → 白堤 → 孤山 → 岳庙', estimated_minutes: 360 },
    { id: 2, name: '九溪徒步线', theme: '自然风光', description: '九溪入口 → 溪中溪 → 茶园', estimated_minutes: 180 },
  ],
  hotProducts: [],
  stats: { totalInteractions: 0, todayInteractions: 0, positiveRate: 0 },
}

const streamingSessions = new Map() // sessionId -> { aborted }

// 失物招领内存数据（与真实后端字段一致：type / title / category / description /
// location / image_url / contact / publisher_name / user_id / status / created_at）
let nextLostFoundId = 1
function addLostFoundType(item) {
  return { id: nextLostFoundId++, status: 'open', created_at: new Date().toISOString(), ...item }
}
const lostFound = [
  addLostFoundType({
    type: 'lost',
    title: '黑色雨伞遗失在断桥附近',
    category: '雨伞',
    description: '长柄黑伞，伞柄有一圈银色装饰，下午在断桥拍照时落在长椅上。',
    location: '断桥残雪',
    contact: '13800000001',
    publisher_name: '演示用户',
    user_id: '1',
  }),
  addLostFoundType({
    type: 'found',
    title: '捡到一串钥匙，挂着橙色小熊',
    category: '钥匙',
    description: '在灵隐寺停车场出口捡到，已交给游客中心，可凭描述认领。',
    location: '灵隐寺停车场',
    contact: '游客中心 0571-88888888',
    publisher_name: '工作人员',
    user_id: '2',
  }),
  addLostFoundType({
    type: 'lost',
    title: '儿童水杯（粉色）不见了',
    category: '水杯',
    description: '粉红色保温杯，杯身贴有卡通贴纸，可能落在雷峰塔二层休息区。',
    location: '雷峰塔',
    contact: '13900000002',
    publisher_name: '演示用户',
    user_id: '1',
  }),
  addLostFoundType({
    type: 'found',
    title: '捡到一张学生卡',
    category: '证件',
    description: '姓名部分已模糊，请失主到九溪游客驿站认领。',
    location: '九溪十八涧',
    contact: '九溪游客驿站',
    publisher_name: '志愿者',
    user_id: '3',
    status: 'resolved',
  }),
]

const CANNED_ANSWER =
  '灵隐寺始建于东晋咸和元年（公元326年），是杭州最早的名刹之一。' +
  '相传印度僧人慧理来到这里，见山峰奇秀，以为“仙灵所隐”，便在此建寺，取名灵隐。' +
  '寺内的大雄宝殿、飞来峰造像都值得细看；建议早上前往，人少且光线好。'

// 第二段只为把流式时长拉到 10s 以上，方便演示「逐字出现」和中途「停止」
const CANNED_ANSWER_MORE =
  '如果要安排半天行程，可以按「飞来峰造像 → 灵隐寺 → 永福寺 → 韬光寺」的顺序走，' +
  '全程约 3 小时，台阶较多，建议穿轻便的鞋。' +
  '门票需要先买飞来峰景区票，再单独购买灵隐寺香花券，两处排队高峰通常在上午十点之后。' +
  '寺内提供素面，中午可以在斋堂简单用餐；返程可沿九溪十八涧步行下山，溪水清凉，夏天尤其舒服。'

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms))
}

async function handleChatStream(req, res) {
  const body = await readBody(req)
  const message = body.message || (Array.isArray(body.messages) ? body.messages.at(-1)?.content : '')
  if (!message) {
    json(res, 400, { error: '请提供 message (string) 或 messages (array) 参数' })
    return
  }

  const sessionId = randomUUID()
  streamingSessions.set(sessionId, { aborted: false })

  res.writeHead(200, {
    'Content-Type': 'text/event-stream; charset=utf-8',
    'Cache-Control': 'no-cache',
    Connection: 'keep-alive',
    'X-Session-Id': sessionId,
    'X-Trace-Id': randomUUID(),
  })

  let seq = 0
  const answer = `关于「${message.trim()}」：${CANNED_ANSWER}${CANNED_ANSWER_MORE}`
  const chunks = answer.match(/[\s\S]{1,3}/g) || []

  for (const chunk of chunks) {
    if (streamingSessions.get(sessionId)?.aborted) break
    res.write(`event: token\nid: ${seq++}\ndata: ${JSON.stringify({ content: chunk })}\n\n`)
    await sleep(90)
  }

  if (streamingSessions.get(sessionId)?.aborted) {
    res.write(`event: error\nid: ${seq++}\ndata: ${JSON.stringify({ message: '已按用户请求停止生成' })}\n\n`)
    res.write(`event: done\nid: ${seq++}\ndata: {}\n\n`)
  } else {
    res.write(`event: done\nid: ${seq++}\ndata: ${JSON.stringify({ fullContent: answer })}\n\n`)
  }
  streamingSessions.delete(sessionId)
  res.end()
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`)
  const path = url.pathname
  const method = req.method

  if (method === 'OPTIONS') {
    res.writeHead(204, { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Headers': '*' })
    return res.end()
  }

  if (path === '/health' && method === 'GET') {
    return ok(res, { status: 'ok', mode: 'mock' })
  }

  if (path === '/api/v1/auth/register' && method === 'POST') {
    const body = await readBody(req)
    if (!body.username || !body.password || body.password.length < 6) {
      return fail(res, 400, 40001, '用户名不能为空，密码至少 6 位')
    }
    if (users.has(body.username)) return fail(res, 409, 40001, '用户名已存在')
    const user = addUser(body.username, body.password, body.nickname)
    return ok(res, { id: user.id }, 201)
  }

  if (path === '/api/v1/auth/login' && method === 'POST') {
    const body = await readBody(req)
    const user = users.get(body.username)
    if (!user || user.password !== body.password) {
      return json(res, 401, { code: 40101, message: '用户名或密码错误' })
    }
    const token = `mock-token-${user.username}`
    issuedTokens.add(token)
    return ok(res, {
      user: { id: user.id, username: user.username, nickname: user.nickname, role: user.role },
      token,
    })
  }

  if (path === '/api/v1/auth/me' && method === 'GET') {
    const user = currentUser(req)
    if (!user) return json(res, 401, { code: 40102, message: 'Token 无效或已过期' })
    return ok(res, { id: user.id, username: user.username, nickname: user.nickname, role: user.role })
  }

  if (path === '/api/v1/home' && method === 'GET') {
    // 真实后端 home 路由没有 requireAuth（见 backend/src/routes/home.ts），这里同样不强制登录
    return ok(res, homeData)
  }

  if (path === '/api/v1/chat/stream' && method === 'POST') {
    return handleChatStream(req, res)
  }

  if (path === '/api/v1/chat/stop' && method === 'POST') {
    const body = await readBody(req)
    if (body.sessionId && streamingSessions.has(body.sessionId)) {
      streamingSessions.get(body.sessionId).aborted = true
    }
    return ok(res, { sessionId: body.sessionId || '', stopped: true })
  }

  if (path === '/api/v1/lost-found' && method === 'GET') {
    const type = url.searchParams.get('type')
    const status = url.searchParams.get('status')
    const keyword = url.searchParams.get('keyword')
    const page = Math.max(1, Number(url.searchParams.get('page') || 1))
    // 真实后端 size 上限 50，默认 10
    const size = Math.min(50, Math.max(1, Number(url.searchParams.get('size') || 10)))

    const rows = lostFound.filter((item) => {
      if (type && item.type !== type) return false
      if (status && item.status !== status) return false
      if (keyword) {
        const haystack = `${item.title} ${item.description || ''} ${item.location || ''}`
        if (!haystack.includes(keyword)) return false
      }
      return true
    })

    const start = (page - 1) * size
    const total = rows.length
    return ok(res, {
      list: rows.slice(start, start + size),
      pagination: { page, size, total, totalPages: Math.ceil(total / size) },
    })
  }

  if (path === '/api/v1/lost-found' && method === 'POST') {
    const body = await readBody(req)
    if (!body.type || !body.title) {
      return fail(res, 400, 40001, 'type 和 title 为必填项')
    }
    if (!['lost', 'found'].includes(body.type)) {
      return fail(res, 400, 40001, 'type 只能是 lost 或 found')
    }
    const item = addLostFoundType({
      type: body.type,
      title: body.title,
      category: body.category || 'other',
      description: body.description || null,
      location: body.location || null,
      image_url: body.image_url || null,
      contact: body.contact || null,
      publisher_name: body.publisher_name || null,
      user_id: body.user_id || null,
    })
    lostFound.unshift(item)
    return ok(res, item, 201)
  }

  const lostFoundDetailMatch = path.match(/^\/api\/v1\/lost-found\/(\d+)$/)
  if (lostFoundDetailMatch && method === 'GET') {
    const item = lostFound.find((row) => row.id === Number(lostFoundDetailMatch[1]))
    if (!item) return fail(res, 404, 40401, '失物招领记录不存在')
    return ok(res, item)
  }
  if (lostFoundDetailMatch && method === 'PUT') {
    const body = await readBody(req)
    const item = lostFound.find((row) => row.id === Number(lostFoundDetailMatch[1]))
    if (!item) return fail(res, 404, 40401, '失物招领记录不存在')
    if (body.type !== undefined && !['lost', 'found'].includes(body.type)) {
      return fail(res, 400, 40001, 'type 只能是 lost 或 found')
    }
    // 真实后端：允许只传部分字段，但至少要有一个合法字段
    const allowed = ['type', 'title', 'category', 'description', 'location', 'contact', 'publisher_name']
    const patch = {}
    for (const key of allowed) if (body[key] !== undefined) patch[key] = body[key]
    if (Object.keys(patch).length === 0) {
      return fail(res, 400, 40001, '至少需要一个可更新字段')
    }
    Object.assign(item, patch, { updated_at: new Date().toISOString() })
    return ok(res, item)
  }
  if (lostFoundDetailMatch && method === 'DELETE') {
    const id = Number(lostFoundDetailMatch[1])
    const idx = lostFound.findIndex((row) => row.id === id)
    if (idx === -1) return fail(res, 404, 40401, '失物招领记录不存在')
    lostFound.splice(idx, 1)
    return ok(res, { id })
  }

  const lostFoundStatusMatch = path.match(/^\/api\/v1\/lost-found\/(\d+)\/status$/)
  if (lostFoundStatusMatch && method === 'PATCH') {
    const body = await readBody(req)
    if (!['open', 'resolved', 'expired'].includes(body.status)) {
      return fail(res, 400, 40001, 'status 只能是 open / resolved / expired')
    }
    const item = lostFound.find((row) => row.id === Number(lostFoundStatusMatch[1]))
    if (!item) return fail(res, 404, 40401, '失物招领记录不存在')
    item.status = body.status
    item.updated_at = new Date().toISOString()
    // 真实后端 PATCH /status 只返回 { id, status }
    return ok(res, { id: item.id, status: item.status })
  }

  // poi 表 category 是英文枚举；首页 hotPois 用中文展示，出接口时映射成后端真实值
  const poiCategoryEnum = { 人文古迹: 'landmark', 自然风光: 'attraction' }
  const toPoiRecord = (p) => ({ ...p, category: poiCategoryEnum[p.category] || 'other', status: 1 })

  if (path === '/api/v1/poi' && method === 'GET') {
    const category = url.searchParams.get('category')
    const rows = homeData.hotPois.map(toPoiRecord)
    return ok(res, category ? rows.filter((p) => p.category === category) : rows)
  }

  // 景点详情：字段与真实后端 poi 表一致，id 非法 400 / 不存在 404
  const poiDetailMatch = path.match(/^\/api\/v1\/poi\/([^/]+)$/)
  if (poiDetailMatch && method === 'GET') {
    const raw = decodeURIComponent(poiDetailMatch[1])
    if (!/^\d+$/.test(raw)) return fail(res, 400, 40001, '景点 id 非法')
    const poi = homeData.hotPois.find((p) => p.id === Number(raw))
    if (!poi) return fail(res, 404, 40401, '景点不存在')
    return ok(res, toPoiRecord(poi))
  }

  fail(res, 404, 40401, `mock 未实现该接口：${method} ${path}`)
})

server.listen(PORT, '0.0.0.0', () => {
  console.log(`mock 后端已启动: http://localhost:${PORT}  (模拟器用 http://10.0.2.2:${PORT})`)
  console.log('预置账号: demo / 123456')
})
