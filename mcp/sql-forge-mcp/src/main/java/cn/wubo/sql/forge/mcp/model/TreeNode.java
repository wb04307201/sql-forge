package cn.wubo.sql.forge.mcp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 树形节点结构
 * 用于表示数据库元数据的树形结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TreeNode<T> {
    /**
     * 节点标签
     */
    private String label;

    /**
     * 节点值
     */
    private String value;

    /**
     * 节点数据
     */
    private T data;

    /**
     * 子节点列表
     */
    private List<TreeNode<?>> children;

    /**
     * 添加子节点
     */
    public void addChild(TreeNode<?> child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }

    /**
     * 判断是否有子节点
     */
    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    /**
     * 获取子节点数量
     */
    public int getChildCount() {
        return children == null ? 0 : children.size();
    }
}
