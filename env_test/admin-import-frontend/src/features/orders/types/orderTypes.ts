export enum OrderStatus {
  PENDING = "PENDING",
  PROCESSING = "PROCESSING",
  CONFIRMED = "CONFIRMED",
  SHIPPED = "SHIPPED",
  DELIVERED = "DELIVERED",
  COMPLETED = "COMPLETED",
  CANCELLED = "CANCELLED",
}

export enum PaymentStatus {
  PENDING = "PENDING",
  SUCCESS = "SUCCESS",
  FAILED = "FAILED",
  REFUNDED = "REFUNDED",
}

export enum CriteriaType {
  ALL = 0,
  USER_ID = 1,
  STATUS = 2,
  USER_ID_STATUS = 3,
  USER_ID_DATE_RANGE = 4,
}

export enum PaymentMethod {
  RAZORPAY = "RAZORPAY",
  SINGE = "SINGE",
  BAKONG = "BAKONG",
  ABA = "ABA",
  ACLEDA = "ACLEDA",
}
