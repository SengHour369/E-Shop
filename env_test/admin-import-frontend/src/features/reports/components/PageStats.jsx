import React from 'react';
import {
  ShoppingCart,
  CheckCircle,
  Clock,
  XCircle,
  AlertTriangle,
  Ban,
  TrendingDown,
  DollarSign,
  PackageX,
  RefreshCcw,
  Percent,
  Banknote,
  Users,
  UserCheck,
  UserPlus,
  UserMinus,
  Package,
  ArchiveX,
  Grid,
  TrendingUp,
  Briefcase,
  Star,
  Activity
} from 'lucide-react';

export const orderStats = [
  {
    label: "Total Orders",
    value: "0",
    badgeText: "",
    badgeColorClass: "text-emerald-500",
    iconBgColorClass: "bg-blue-50 text-blue-500 rounded-full border border-blue-100",
    icon: <DollarSign size={24} />
  },
  {
    label: "Completed Orders",
    value: "0",
    badgeText: "",
    badgeColorClass: "text-emerald-500",
    iconBgColorClass: "bg-emerald-50 text-emerald-500 rounded-full border border-emerald-100",
    icon: <ShoppingCart size={24} />
  },
  {
    label: "Pending Orders",
    value: "0",
    badgeText: "",
    badgeColorClass: "text-emerald-500",
    iconBgColorClass: "bg-amber-50 text-amber-500 rounded-full border border-amber-100",
    icon: <Clock size={24} />
  },
  {
    label: "Cancelled Orders",
    value: "0",
    badgeText: "",
    badgeColorClass: "text-red-500",
    iconBgColorClass: "bg-red-50 text-red-500 rounded-full border border-red-100",
    icon: <XCircle size={24} />
  }
];

export const cancelationStats = [
  {
    label: "Total Cancellations",
    value: "842",
    badgeText: "-2.4%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-red-100 text-red-600",
    icon: <Ban size={24} />
  },
  {
    label: "Pending Review",
    value: "145",
    badgeText: "+12.0%",
    badgeColorClass: "text-amber-700 bg-amber-100",
    iconBgColorClass: "bg-amber-100 text-amber-600",
    icon: <AlertTriangle size={24} />
  },
  {
    label: "Cancelation Rate",
    value: "3.4%",
    badgeText: "-0.5%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-purple-100 text-purple-600",
    icon: <TrendingDown size={24} />
  },
  {
    label: "Value Lost",
    value: "$42,500",
    badgeText: "-5.2%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-red-100 text-red-600",
    icon: <DollarSign size={24} />
  }
];

export const returnStats = [
  {
    label: "Total Returns",
    value: "1,204",
    badgeText: "-1.8%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-blue-100 text-blue-600",
    icon: <PackageX size={24} />
  },
  {
    label: "Processed Returns",
    value: "1,050",
    badgeText: "+4.2%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-emerald-100 text-emerald-600",
    icon: <CheckCircle size={24} />
  },
  {
    label: "Pending Returns",
    value: "154",
    badgeText: "-8.5%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-amber-100 text-amber-600",
    icon: <Clock size={24} />
  },
  {
    label: "Return Rate",
    value: "4.8%",
    badgeText: "-0.2%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-purple-100 text-purple-600",
    icon: <Percent size={24} />
  }
];

export const refundStats = [
  {
    label: "Total Refunds",
    value: "956",
    badgeText: "-3.1%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-blue-100 text-blue-600",
    icon: <RefreshCcw size={24} />
  },
  {
    label: "Completed Refunds",
    value: "890",
    badgeText: "+5.4%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-emerald-100 text-emerald-600",
    icon: <CheckCircle size={24} />
  },
  {
    label: "Pending Refunds",
    value: "66",
    badgeText: "-12.5%",
    badgeColorClass: "text-red-700 bg-red-100",
    iconBgColorClass: "bg-amber-100 text-amber-600",
    icon: <Clock size={24} />
  },
  {
    label: "Refunded Amount",
    value: "$124,500",
    badgeText: "-2.8%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-red-100 text-red-600",
    icon: <Banknote size={24} />
  }
];

export const customerStats = [
  {
    label: "Total Customers",
    value: "145k",
    badgeText: "+14.5%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-blue-100 text-blue-600",
    icon: <Users size={24} />
  },
  {
    label: "Active Customers",
    value: "89k",
    badgeText: "+22.4%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-emerald-100 text-emerald-600",
    icon: <UserCheck size={24} />
  },
  {
    label: "New Customers",
    value: "12,450",
    badgeText: "+8.2%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-purple-100 text-purple-600",
    icon: <UserPlus size={24} />
  },
  {
    label: "At Risk Customers",
    value: "3,240",
    badgeText: "-5.4%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-amber-100 text-amber-600",
    icon: <UserMinus size={24} />
  }
];

export const productStats = [
  {
    label: "Total Products",
    value: "8,450",
    badgeText: "+4.5%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-blue-100 text-blue-600",
    icon: <Package size={24} />
  },
  {
    label: "Active Products",
    value: "7,120",
    badgeText: "+6.2%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-emerald-100 text-emerald-600",
    icon: <CheckCircle size={24} />
  },
  {
    label: "Low Stock",
    value: "450",
    badgeText: "-12.5%",
    badgeColorClass: "text-red-700 bg-red-100",
    iconBgColorClass: "bg-amber-100 text-amber-600",
    icon: <AlertTriangle size={24} />
  },
  {
    label: "Out of Stock",
    value: "125",
    badgeText: "-8.4%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-red-100 text-red-600",
    icon: <ArchiveX size={24} />
  }
];

export const categoryStats = [
  {
    label: "Total Categories",
    value: "145",
    badgeText: "+2.5%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-blue-100 text-blue-600",
    icon: <Grid size={24} />
  },
  {
    label: "Active Categories",
    value: "132",
    badgeText: "+3.4%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-emerald-100 text-emerald-600",
    icon: <CheckCircle size={24} />
  },
  {
    label: "Top Performing",
    value: "24",
    badgeText: "+12.5%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-purple-100 text-purple-600",
    icon: <TrendingUp size={24} />
  },
  {
    label: "Inactive Categories",
    value: "13",
    badgeText: "-2.4%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-amber-100 text-amber-600",
    icon: <Ban size={24} />
  }
];

export const brandStats = [
  {
    label: "Total Brands",
    value: "450",
    badgeText: "+8.5%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-blue-100 text-blue-600",
    icon: <Briefcase size={24} />
  },
  {
    label: "Active Brands",
    value: "412",
    badgeText: "+10.2%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-emerald-100 text-emerald-600",
    icon: <CheckCircle size={24} />
  },
  {
    label: "Top Selling",
    value: "35",
    badgeText: "+15.4%",
    badgeColorClass: "text-emerald-700 bg-emerald-100",
    iconBgColorClass: "bg-purple-100 text-purple-600",
    icon: <Star size={24} />
  },
  {
    label: "Pending Approval",
    value: "24",
    badgeText: "-5.2%",
    badgeColorClass: "text-red-700 bg-red-100",
    iconBgColorClass: "bg-amber-100 text-amber-600",
    icon: <Activity size={24} />
  }
];
