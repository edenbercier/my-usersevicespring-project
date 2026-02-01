package com.appsdeveloperblog.tutorials.junit.security;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class RolePermissions {

  private RolePermissions() {}

  private static final Map<String, List<String>> ROLE_TO_PERMISSIONS =
      Map.of(
          "admin", List.of(
              "user.create",
              "user.read",
              "user.update",
              "user.delete"
          ),
          "viewer", List.of(
              "user.read"
          ),
          "guest", List.of(
              "user.read"
          )
      );

  public static List<String> permissionsFor(List<String> roles) {
    return roles.stream()
        .flatMap(role -> ROLE_TO_PERMISSIONS
            .getOrDefault(role.toLowerCase(), List.of())
            .stream())
        .distinct()
        .collect(Collectors.toList());
  }
}
