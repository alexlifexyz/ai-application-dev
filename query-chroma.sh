#!/bin/bash

# Chroma 向量数据库查询脚本
# 用于查看 ai-knowledge collection 中存储的知识数据

CHROMA_URL="http://localhost:8000"
COLLECTION_ID="ec1ff850-ed7d-4ddc-8e1d-d05d803a41ac"

echo "=========================================="
echo "Chroma 向量数据库查询"
echo "=========================================="
echo ""

# 1. 查询文档数量
echo "📊 文档数量："
COUNT=$(curl -s -X POST "${CHROMA_URL}/api/v2/collections/${COLLECTION_ID}/count")
echo "   ${COUNT} 个文档"
echo ""

# 2. 查询 collection 基本信息
echo "📁 Collection 信息："
curl -s "${CHROMA_URL}/api/v2/tenants/default/databases/default/collections/ai-knowledge" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(f\"   名称: {data.get('name', 'N/A')}\")
    print(f\"   ID: {data.get('id', 'N/A')}\")
    print(f\"   向量维度: {data.get('dimension', 'N/A')}\")
    print(f\"   相似度算法: {data.get('metadata', {}).get('hnsw:space', 'N/A')}\")
except:
    print('   (无法解析)')
"
echo ""

# 3. 获取前 5 条文档
echo "📄 最近存储的文档（前 5 条）："
curl -s -X POST "${CHROMA_URL}/api/v2/collections/${COLLECTION_ID}/get" \
  -H "Content-Type: application/json" \
  -d '{"limit": 5, "include": ["documents", "metadatas"]}' | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    docs = data.get('documents', [])
    metadatas = data.get('metadatas', [])
    ids = data.get('ids', [])
    
    if not docs:
        print('   (暂无文档)')
    else:
        for i, (doc_id, doc, meta) in enumerate(zip(ids, docs, metadatas), 1):
            print(f'   [{i}] ID: {doc_id}')
            if meta:
                source = meta.get('source', 'unknown')
                print(f'       来源: {source}')
            # 截断显示文档内容
            content = doc[:100] + '...' if len(doc) > 100 else doc
            print(f'       内容: {content}')
            print()
except Exception as e:
    print(f'   (查询失败: {e})')
"

echo "=========================================="
echo "💡 提示："
echo "   - Collection 名称: ai-knowledge"
echo "   - Chroma 服务: ${CHROMA_URL}"
echo "   - API 文档: ${CHROMA_URL}/docs"
echo "=========================================="
