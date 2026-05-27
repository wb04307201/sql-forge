package cn.wubo.sql.forge;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用树形节点，用于表示数据库元数据的层级结构（数据库 → Schema → 表）。
 *
 * @param <T> 节点携带的数据类型
 */
@Data
public class TreeNode<T> {
    private String label;
    private String value;
    private T data;
    private List<TreeNode<?>> children;

    /**
     * 添加子节点，首次调用时延迟初始化子节点列表。
     *
     * @param child 子节点
     */
    public void addChild(TreeNode<?> child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }
}
