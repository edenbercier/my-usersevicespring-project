package com.appsdeveloperblog.tutorials.junit;

import java.util.List;

public class Rbac {

    public static boolean hasPermission(String role, String permission) {
      if ("admin".equals(role)) return true;

      if ("editor".equals(role)) {
        return List
            .of(
            "post.create",
            "post.update",
            "post.delete"
        ).contains(permission);
      }

      if ("viewer".equals(role)) {
        return List.of(
            "post.read"
        ).contains(permission);
      }

      if ("guest".equals(role)) {
        return List.of(
            "post.read",
            "user.create"
        ).contains(permission);
      }

      return false;
    }
  }


