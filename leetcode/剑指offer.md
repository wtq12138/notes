# 二叉树

## 前序和中序返回整个树

函数里要有前序和中序的范围，显然在中序中找到前序的位置可以计算出左子树结点的个数，

向上传递根节点

```c++
class Solution {
    unordered_map<int,int> pos;
    TreeNode* solve(int l1,int r1,int l2,int r2,vector<int> pre,vector<int> ino)
    {
        if(l2>r2) return NULL;
        int mid=pos[pre[l1]];
        int cnt=mid-l2;
        TreeNode * tmp=new TreeNode;
        tmp->val=pre[l1];
        tmp->left=solve(l1+1,l1+cnt,l2,mid-1,pre,ino);
        tmp->right=solve(l1+cnt+1,r1,mid+1,r2,pre,ino);
        return tmp;
    }
public:
    TreeNode* buildTree(vector<int>& preorder, vector<int>& inorder) {
        for(int i=0;i<inorder.size();i++)
        pos[inorder[i]]=i;
        return solve(0,preorder.size()-1,0,preorder.size()-1,preorder,inorder);
    }
};
```

## 树的子结构

特别的是要求B如果为空，则不认为是A的子树

这个条件加上就不能一个函数解决了因为B为空可能是一开始就为空，也有可能是遍历结束后为空

分为两个函数

一个函数判断当前根节点是否可以作为B的子树起点

一个函数判断是否满足从当前根节点开始是否为B完全一样

```c++
class Solution {
public:
    bool isSubStructure(TreeNode* A, TreeNode* B) {
        return (A!=NULL&&B!=NULL)&&(dfs(A,B)||isSubStructure(A->left,B)||isSubStructure(A->right,B));
    }
    bool dfs(TreeNode* A, TreeNode* B) {
        if(B==NULL) return true;
        if(A==NULL||A->val!=B->val) return false;
        return dfs(A->left,B->left)&&dfs(A->right,B->right);
        
    }
};
```

## 镜像二叉树

判断是否是镜像

当A结点与B结点相等时还需要满足的条件是A的左结点等于B的右节点，A的右节点等于B的左节点

可以递归

```c++
class Solution {
public:
    bool isSymmetric(TreeNode* root) {
        return !root||pd(root->left,root->right);
    }
    bool pd(TreeNode *l,TreeNode *r)
    {
        if(l==NULL&&r==NULL)
            return true;
        else if(l!=NULL&&r!=NULL&&l->val==r->val)
            return pd(l->left,r->right)&&pd(l->right,r->left);
        else
            return false;
    }
};
```

## 二叉搜索数的后序遍历

二叉搜索数的性质中序遍历是顺序，而先序遍历和后序遍历中划分左右子树的依据是大于小于根节点

注意递归参数 r-1把根节点跳过

```c++
 bool dfs(int l,int r,vector<int> a)
    {
        if(l>=r)
            return 1;
        int mid=a[r];
        int pos=l;
        while(pos<=r&&a[pos]<mid)
        pos++;
        int midpos=pos-1;
        while(pos<=r&&a[pos]>mid)
        pos++;
        return pos==r&&dfs(l,midpos,a)&&dfs(midpos+1,r-1,a);
    }
```



# 两个栈模拟队列

插入一个栈1，删除一个栈2，删除前看2有没有就出栈，不然把1出栈填到2中，如果1页没有返回-1

# 剪绳子

转移方程

```
dp[i]=max(j*max(i-j,dp[i-j]),dp[i]);
```

# 反转链表递归

迭代记录前一个和后一个 移动即可

返回值是末尾最后一个结点只需要找到然后原封不动返回即可，过程中修改指针方向

```c++
class Solution {
public:
    ListNode* reverseList(ListNode* head) {
       if(head==NULL||head->next==NULL)
        return head;
       ListNode *p=reverseList(head->next);
       head->next->next=head;
       head->next=NULL;
       return p;
    }
};
```

# 合并链表递归

递归函数返回前面结点该指向的结点

如果为空了显然指向另一个，否则指向小的

```c++
class Solution {
public:
    ListNode* mergeTwoLists(ListNode* l1, ListNode* l2) {
        if(l1==NULL) return l2;
        if(l2==NULL) return l1;
        if(l1->val<=l2->val){ l1->next=mergeTwoLists(l1->next,l2);return l1;}
        else {l2->next=mergeTwoLists(l1,l2->next);return l2;}
    }
};
```

# 反转k个链表

# 合并k个链表
