package cn.wubo.sql.forge;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TreeNode<T> {
    private String label;
    private String value;
    private T data;
    private List<TreeNode<?>> children;

    public void addChild(TreeNode<?> child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }
}
