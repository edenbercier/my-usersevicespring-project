package com.appsdeveloperblog.tutorials.junit;


import static com.appsdeveloperblog.tutorials.junit.Permission.USER_CREATE;
import static com.appsdeveloperblog.tutorials.junit.Permission.USER_DELETE;
import static com.appsdeveloperblog.tutorials.junit.Permission.USER_UPDATE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class RbacPermissionTest {

  @Test
  public void testGuestCannotDeleteUser() {
    assertFalse(Rbac.hasPermission("guest", USER_DELETE));
  }

  @Test
  public void testViewerCannotUpdateUser() {
    assertFalse(Rbac.hasPermission("viewer", USER_UPDATE));
  }
  @Test
  public void testViewerCannotCreateUser() {
    assertFalse(Rbac.hasPermission("viewer", USER_CREATE));
  }
   @Test
   public void testAdminCanDeleteUser() {
    assertTrue(Rbac.hasPermission("admin", USER_DELETE));
}

  @Test
  public void testEditorCannotDeleteUser() {
    assertFalse(Rbac.hasPermission("editor", USER_DELETE));
  }
}
