package com.example.eshop.catalog.controller;
import com.example.eshop.catalog.service.BranchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/branches") @RequiredArgsConstructor @Validated
public class BranchController {
 private final BranchService service;
 public record Create(@NotBlank @Size(max=120) String name,@Size(max=500) String address){}
 public record Grant(@Positive long userId,boolean active){}
 @GetMapping public Object list(@RequestParam(defaultValue="0") @Min(0) int page){return service.list(page);}
 @PostMapping public Object create(@Valid @RequestBody Create request){return service.create(request.name(),request.address());}
 @GetMapping("/{id}/access") public Object members(@PathVariable @Positive long id){return service.members(id);}
 @PutMapping("/{id}/access") public Object grant(@PathVariable @Positive long id,@Valid @RequestBody Grant request){return service.grant(id,request.userId(),request.active());}
 @PutMapping("/{id}/inventory/{inventoryId}") public void assign(@PathVariable @Positive long id,@PathVariable @Positive long inventoryId){service.assignInventory(id,inventoryId);}
 @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class,org.springframework.dao.OptimisticLockingFailureException.class})
 public org.springframework.http.ResponseEntity<?> conflict(){return org.springframework.http.ResponseEntity.status(409).body(java.util.Map.of("message","Branch or access changed concurrently. Reload before continuing."));}
}
