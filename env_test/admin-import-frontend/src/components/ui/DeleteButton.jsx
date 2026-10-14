import React from 'react';
import Swal from 'sweetalert2';
import { HiTrash } from 'react-icons/hi2';

export default function DeleteButton({ onConfirm, className = "" }) {

    const handleTriggerDelete = async () => {
        Swal.fire({
            title: 'Are you sure?',
            text: 'This data will be deleted permanently!',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d33',
            cancelButtonColor: '#3085d6',
            confirmButtonText: 'Yes, delete it!',
            cancelButtonText: 'Cancel',
            reverseButtons: true
        }).then(async (result) => {
            if (!result.isConfirmed) return;

            try {
                await onConfirm?.();
            } catch (error) {
                Swal.fire({
                    title: 'Delete failed',
                    text: error?.message || 'Unable to delete data.',
                    icon: 'error'
                });
            }
        });
    };

    return (
        <button
            onClick={handleTriggerDelete}
            className={`text-red-600 hover:text-red-900 transition-colors ${className}`}
            title="Delete item"
        >
            <HiTrash className="h-4 w-4" />
        </button>
    );
}