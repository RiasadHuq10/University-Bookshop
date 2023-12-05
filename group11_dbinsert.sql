CREATE TABLE Student
(
sid INTEGER,
name VARCHAR(20),
gender CHAR(1),
major VARCHAR(30),
discount_level FLOAT,
primary key (sid)
);

CREATE TABLE Total_Purchase
(
Sid INTEGER NOT NULL,
purchased FLOAT,
Primary key (sid),
Foreign key (sid) references Student on Delete Cascade
);

CREATE TABLE Book
(
bid INTEGER,
title VARCHAR(80),
author VARCHAR(30),
price FLOAT,
amount INTEGER,
primary key (bid)
);

CREATE TABLE Student_Orders
(
oid INTEGER,
sid INTEGER NOT NULL,
order_date DATE,
total_price FLOAT,
payment_method VARCHAR(30),
card_no CHAR(20),
primary key (oid),
foreign key (sid) REFERENCES Student
);

CREATE TABLE Ordered_Book
(
delivery_date DATE,
delivered INTEGER,
bid INTEGER,
oid INTEGER,
quantity INTEGER,
primary key (bid,oid),
Foreign key (bid) REFERENCES Book,
Foreign key(oid) REFERENCES Student_Orders
);


CREATE or REPLACE TRIGGER credit_card_trigger 
BEFORE INSERT ON Student_Orders
FOR EACH ROW 
BEGIN
IF (:new.payment_method = 'CREDITCARD' AND :new.card_no IS NULL) THEN
RAISE_APPLICATION_ERROR (-20000, 'Card number is missing');
END IF;
END;

.
/


CREATE OR REPLACE TRIGGER AMOUNTOFBOOKDECREASED
BEFORE INSERT ON  Ordered_Book
For each row
Declare a Integer; 
Begin
select amount INTO a from book where book.bid=:new.bid;
   If  (a< :new.quantity) then
	RAISE_APPLICATION_ERROR (-20001, 'Not enough books available');
ELSE
 UPDATE Book set amount=amount-:new.quantity WHERE book.bid=:new.bid;
END IF;
END;
.
/



CREATE OR REPLACE TRIGGER AMOUNTOFBOOKINCREASED
AFTER DELETE ON Ordered_Book
For each row
Begin
UPDATE Book set amount = amount + :old.quantity WHERE book.bid=:old.bid;
END;

.
/



CREATE OR REPLACE TRIGGER deletion_rejection_trigger
BEFORE DELETE ON Student_Orders
FOR EACH ROW
DECLARE
c INTEGER;
BEGIN
SELECT SUM(delivered) into c FROM Ordered_Book WHERE oid = :old.oid;
IF (c > 0) THEN
		RAISE_APPLICATION_ERROR (-20003, 'Cannot cancel the partially delivered order');
ELSIF ( (TRUNC(SYSDATE-:old.order_date)>7)) THEN
	Raise_application_error (-20003, 'More than 7 days passed. Cannot cancel it' );
ELSE
	DELETE from Ordered_Book where Ordered_book.oid = :old.oid;
END IF;
END;


.
/


CREATE OR REPLACE TRIGGER Order_Price_Trigger
AFTER INSERT ON Ordered_Book
FOR EACH ROW
DECLARE
p FLOAT;
BEGIN
SELECT price INTO p FROM Book WHERE Book.bid = :new.bid;
UPDATE Student_Orders
SET total_price = total_price + p*:new.quantity
WHERE Student_Orders.oid = :new.oid;
END;

.
/


CREATE OR REPLACE TRIGGER Total_Purchase_Trigger
AFTER INSERT ON Student
FOR EACH ROW
DECLARE
p integer := :new.sid;
BEGIN
Insert into total_purchase values (:new.sid, 0);
END;

.
/



CREATE OR REPLACE TRIGGER Order_Price_Trigger_Delete
AFTER DELETE ON Student_Orders
FOR EACH ROW
BEGIN
UPDATE  total_purchase
SET purchased = purchased - :old.total_price
Where total_purchase.sid = :old.sid;
END;

.
/



CREATE OR REPLACE TRIGGER discount_trigger
AFTER UPDATE ON total_purchase
FOR EACH ROW
BEGIN
If  (:new.purchased>2000) then
		UPDATE student
		Set discount_level = 20
		Where student.sid = :new.sid;
End if;
	 If  (:new.purchased>1000 AND :new.purchased<=2000) then
		UPDATE student
		Set discount_level = 10
		Where student.sid = :new.sid;	
End if;
 If  (:new.purchased<=1000) then
		UPDATE student
		Set discount_level = 0
		Where student.sid = :new.sid;
	
END IF;
END;

.
/



CREATE OR REPLACE TRIGGER outstanding
BEFORE INSERT ON student_orders
FOR EACH ROW
DECLARE
	d integer;
	C integer :=0;

BEGIN
SELECT sum(delivered) into d from
(select delivered from ordered_book 
Where ordered_book.oid IN (Select oid from student_orders
Where sid =:new.sid)) ;
SELECT count(delivered) into c from
(select delivered from ordered_book 
Where ordered_book.oid IN (Select oid from student_orders
Where sid =:new.sid)) ;
If  (d<>c) then
RAISE_APPLICATION_ERROR (-20004, 'Student has outstanding orders');
END IF;
END;

.
/




INSERT INTO STUDENT VALUES (2131, 'Kurt', 'M',  'CS', 10);
INSERT INTO STUDENT VALUES (2132, 'Rex', 'M',  'CS', 0);
INSERT INTO STUDENT VALUES (2133, 'Jerry', 'M',  'BBA', 0);


UPDATE Total_Purchase SET purchased = 1900 WHERE sid = 2131;
update total_purchase set purchased = 900 where sid = 2132;
update total_purchase set purchased = 300 where sid = 2133;


Insert into book values (101, 'Harry Potter I','A', 300, 11 );
Insert into book values (102, 'Harry Potter II','Ab', 300, 2 );
Insert into book values (103, 'Harry Potter III','Abc', 400, 20 );


INSERT INTO STUDENT_ORDERS VALUES ( 1,  2132,  sysdate - 3, 0,'CASH', NULL);

INSERT INTO STUDENT_ORDERS VALUES ( 2,  2131,  sysdate - 8, 0,'CASH',null);


INSERT INTO Ordered_Book VALUES ( '', 0, 101, 1,1);
INSERT INTO Ordered_Book VALUES ( '', 0, 102, 1,1);
INSERT INTO Ordered_Book VALUES ( '', 0, 102, 2,1);

UPDATE ORDERED_BOOK SET delivered = 1 where OID = 1 and BID = 101;
UPDATE ORDERED_BOOK SET delivery_date = SYSDATE where OID = 1 and BID = 101;

commit;
