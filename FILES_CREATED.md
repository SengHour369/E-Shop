        +--------+          +--------------+          +-----------+
        | Users  |          | User_Group   |          | Groups    |
        +--------+          +--------------+          +-----------+
        | id     |<-------> | user_id      | <------> | id        |
        | name   |          | group_id     |          | name      |
        +--------+          +--------------+          +-----------+
                                                                    |
                                                                    |
                                                                    |
                                                                    +-------------------+
                                                                    | Group_Permission |
                                                                    +-------------------+
                                                                    | group_id          |
                                                                    | permission_id     |
                                                                    +-------------------+
                                                                    |
                                                                    |
                                                                    +--------------+
                                                                    | Permissions  |
                                                                    +--------------+
                                                                    | id           |
                                                                    | code         |
                                                                    | description  |
                                                                    +--------------+







                Group Permission

                A group contains many permissions.
                
                Example:
                
                Admin
                CUSTOMER_CREATE
                CUSTOMER_VIEW
                CUSTOMER_UPDATE
                CUSTOMER_DELETE
                LOAN_APPROVE
                USER_MANAGE
                Loan Officer
                CUSTOMER_VIEW
                CUSTOMER_CREATE
                LOAN_APPROVE
                Teller
                CUSTOMER_VIEW
                CASH_DEPOSIT
                CASH_WITHDRAW

                                                                    

                        Alice
                        │
                        ├── Admin
                        └── Auditor
                        
                        Bob
                        │
                        └── Loan Officer
                        
                        Charlie
                        │
                        └── Teller