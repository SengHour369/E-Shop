package com.example.eshop.admin.controller;
import com.example.eshop.admin.livetest.LiveTestService;
import com.example.eshop.common.security.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@RestController @RequestMapping("/api/v1/admin/live-tests/runs") @RequiredArgsConstructor
public class LiveTestController {
 private final LiveTestService service;
 private final LivePermissionService permissions;
 public record Start(boolean guestSession) {}
 @ModelAttribute void preventCaching(HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
 @GetMapping public List<LiveTestService.Summary> history(){permissions.require("SYSTEM_TEST_VIEW");return service.history(CurrentActor.userId());}
 @GetMapping("/{id}") public LiveTestService.Report get(@PathVariable UUID id){permissions.require("SYSTEM_TEST_VIEW");return service.get(id,CurrentActor.userId());}
 @PostMapping public ResponseEntity<LiveTestService.Report> start(@RequestBody(required=false) Start options,HttpServletRequest request){
  permissions.require("SYSTEM_TEST_RUN");permissions.require("SYSTEM_TEST_VIEW");
  String bearer=request.getHeader("Authorization");if(bearer==null||!bearer.startsWith("Bearer "))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  return ResponseEntity.accepted().body(service.start(CurrentActor.userId(),bearer,request.getHeader("Origin"),options!=null&&options.guestSession()));
 }
}
