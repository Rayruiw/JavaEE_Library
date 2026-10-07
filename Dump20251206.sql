-- MySQL dump 10.13  Distrib 8.0.44, for macos15 (arm64)
--
-- Host: localhost    Database: librarydb
-- ------------------------------------------------------
-- Server version	8.4.7

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `book_category`
--

DROP TABLE IF EXISTS `book_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_category` (
  `ID` bigint NOT NULL AUTO_INCREMENT,
  `CODE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL,
  `DESCRIPTION` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `NAME` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `CODE` (`CODE`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_category`
--

LOCK TABLES `book_category` WRITE;
/*!40000 ALTER TABLE `book_category` DISABLE KEYS */;
INSERT INTO `book_category` VALUES (1,'A','General works','GENERAL WORKS'),(2,'B','Philosophy, psychology and religion','PHILOSOPHY. PSYCHOLOGY. RELIGION'),(3,'C','Auxiliary sciences of history','AUXILIARY SCIENCES OF HISTORY'),(4,'D','General and European history','HISTORY (GENERAL) AND HISTORY OF EUROPE'),(5,'E','History of America','HISTORY: AMERICA'),(6,'F','Local history of the United States and Latin America','HISTORY: AMERICA'),(7,'G','Geography, anthropology, recreation','GEOGRAPHY. ANTHROPOLOGY. RECREATION'),(8,'H','Social sciences','SOCIAL SCIENCES'),(9,'J','Political science','POLITICAL SCIENCE'),(10,'K','Law','LAW'),(11,'L','Education','EDUCATION'),(12,'M','Music','MUSIC AND BOOKS ON MUSIC'),(13,'N','Fine arts','FINE ARTS'),(14,'P','Language and literature','LANGUAGE AND LITERATURE'),(15,'Q','Science','SCIENCE'),(16,'R','Medicine','MEDICINE'),(17,'S','Agriculture','AGRICULTURE'),(18,'T','Technology','TECHNOLOGY'),(19,'U','Military science','MILITARY SCIENCE'),(20,'V','Naval science','NAVAL SCIENCE'),(21,'Z','Library and information science','BIBLIOGRAPHY. LIBRARY SCIENCE. INFORMATION RESOURCES (GENERAL)');
/*!40000 ALTER TABLE `book_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book_copy`
--

DROP TABLE IF EXISTS `book_copy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_copy` (
  `ID` bigint NOT NULL AUTO_INCREMENT,
  `book_id` bigint DEFAULT NULL,
  `status_id` bigint DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_book_copy_book_id` (`book_id`),
  KEY `FK_book_copy_status_id` (`status_id`),
  CONSTRAINT `FK_book_copy_book_id` FOREIGN KEY (`book_id`) REFERENCES `books` (`ID`),
  CONSTRAINT `FK_book_copy_status_id` FOREIGN KEY (`status_id`) REFERENCES `book_copy_status` (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_copy`
--

LOCK TABLES `book_copy` WRITE;
/*!40000 ALTER TABLE `book_copy` DISABLE KEYS */;
INSERT INTO `book_copy` VALUES (1,1,5),(2,1,1),(4,2,1),(5,6,2),(6,3,1),(8,6,1),(9,9,3),(10,5,2),(11,8,1),(12,8,1),(13,4,1),(14,1,5),(15,3,1),(16,1,2);
/*!40000 ALTER TABLE `book_copy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book_copy_status`
--

DROP TABLE IF EXISTS `book_copy_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_copy_status` (
  `ID` bigint NOT NULL AUTO_INCREMENT,
  `CODE` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `DESCRIPTION` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `NAME` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `CODE` (`CODE`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_copy_status`
--

LOCK TABLES `book_copy_status` WRITE;
/*!40000 ALTER TABLE `book_copy_status` DISABLE KEYS */;
INSERT INTO `book_copy_status` VALUES (1,'AVAILABLE','The copy is available to borrow.','Available'),(2,'BORROWED','The copy is currently borrowed by a user.','Borrowed'),(3,'LOST','The copy has been reported lost.','Lost'),(4,'DAMAGED','The copy is damaged and not available.','Damaged'),(5,'PENDING','Pending for librarian approval','Pending');
/*!40000 ALTER TABLE `book_copy_status` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `books`
--

DROP TABLE IF EXISTS `books`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `books` (
  `ID` bigint NOT NULL AUTO_INCREMENT,
  `AUTHOR` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `COVERIMAGEURL` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ISBN` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PUBLISHYEAR` int DEFAULT NULL,
  `PUBLISHER` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TITLE` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_id` bigint DEFAULT NULL,
  `ACTIVE` tinyint(1) NOT NULL DEFAULT '0',
  `description` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`ID`),
  KEY `FK_books_category_id` (`category_id`),
  CONSTRAINT `FK_books_category_id` FOREIGN KEY (`category_id`) REFERENCES `book_category` (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `books`
--

LOCK TABLES `books` WRITE;
/*!40000 ALTER TABLE `books` DISABLE KEYS */;
INSERT INTO `books` VALUES (1,' J. K. Rowling','/images/books/Harry_Potter.jpg','1338596705',2020,'Scholastic Inc.','Harry Potter and the Sorcerer\'s Stone (Harry Potter, Book 1) (MinaLima Edition) (Interactive Illustrated Edition)',11,1,'A dazzling new edition of J.K. Rowling\'s Harry Potter and the Sorcerer\'s Stone, fully illustrated in brilliant color and featuring exclusive interactive paper craft elements, including a foldout Hogwarts letter and more!\r\n\r\nIn this stunning new edition of Harry Potter and the Sorcerer\'s Stone, experience the story as never before. J.K. Rowling\'s complete and unabridged text is accompanied by full-color illustrations on nearly every page and eight exclusive, interactive paper craft elements: Readers will open Harry\'s Hogwarts letter, reveal the magical entryway to Diagon Alley, make a sumptuous feast appear in the Great Hall, and more.\r\n\r\nDesigned and illustrated by award-winning design studio MinaLima -- best known for establishing the visual graphic style of the Harry Potter and Fantastic Beasts films -- this edition is sure to be a keepsake for Harry Potter fans, a beautiful addition to any collector\'s bookshelf, and an enchanting way to introduce the first book in this beloved series to a new generation of readers.'),(2,'RayW','/images/Image-Placeholder.png','11111809380',NULL,'','Java Book',7,0,NULL),(3,'Evi Nemeth, Garth Snyder ','/images/books/Linux.jpg','0134277554',2017,'Prentice Hall Press','UNIX and Linux System Administration Handbook',15,1,'“As an author, editor, and publisher, I never paid much attention to the competition―except in a few cases. This is one of those cases. The UNIX System Administration Handbook is one of the few books we ever measured ourselves against.”\r\n\r\n―Tim O’Reilly, founder of O’Reilly Media\r\n\r\n“This edition is for those whose systems live in the cloud or in virtualized data centers; those whose administrative work largely takes the form of automation and configuration source code; those who collaborate closely with developers, network engineers, compliance officers, and all the other worker bees who inhabit the modern hive.”\r\n\r\n―Paul Vixie, Internet Hall of Fame-recognized innovator and founder of ISC and Farsight Security\r\n\r\n“This book is fun and functional as a desktop reference. If you use UNIX and Linux systems, you need this book in your short-reach library. It covers a bit of the systems’ history but doesn’t bloviate. It’s just straight-forward information delivered in a colorful and memorable fashion.”\r\n\r\n―Jason A. Nunnelley'),(4,'DK','/images/books/cat.jpg','1379873942',2023,'DK Children','My Book of Cats and Kittens: A Fact-Filled Guide to Your Feline Friends',11,1,'Learn everything there is to know about our furry feline friends in this adorable book of cats for children. Discover fascinating information about cats and kittens – from their hair type, fur pattern, colors, and distinct personalities, to the differences and similarities that celebrate the unique identity of every feline!\r\n\r\nChildren aged 5-7 can learn about the profiles of 45 different cat breeds, such as the British Shorthair, Bombay, Siamese, Munchkin, Snowshoe, and more. Young cat lovers will enjoy finding out how cats evolved from wild animals to become pets, and get an insight into their eating habits and preferences as well as how to care for them.'),(5,'Andrew Surmani ','/images/books/music.jpg','978088284897',1998,'Alfred Music','Alfred\'s Essentials of Music Theory: Complete, Book & 2 CDs',12,1,'Alfred\'s Essentials of Music Theory is designed for students of any age, whether listeners or performers, who want to have a better understanding of the language of music. In this all-in-one theory course, you will learn the essentials of music through concise lessons, practice your music reading and writing skills in the exercises, improve your listening skills with the available ear-training CDs (included), and test your knowledge with a review that completes each unit.\r\n'),(6,'Swift Learning Publication','/images/books/java.jpg','979833797447',2024,'Independently published','Java for Beginners: Build Your Dream Tech Career with Engaging Lessons and Projects',15,1,'Start your journey with Java for Beginners—your ultimate guide to understanding and applying Java programming!\r\nAre you feeling overwhelmed by complex programming languages and unsure where to begin?\r\nDo concepts like Object-Oriented Programming and data structures seem confusing?\r\nStruggling to balance theoretical knowledge with practical application?\r\nJava for Beginners is designed with you in mind. This book takes a beginner-friendly, structured approach, offering clear\r\nexplanations, practical examples, and engaging hands-on exercises to help you build confidence and expertise.'),(7,'d','/images/books/1764610116659_os-book.jpg','1289042094784',1000,'jofjspj','ss',3,0,''),(8,'Aristeidis Bampakos ','/images/books/Angular.jpg','1835087485',2025,'Packt Publishing','Learning Angular: A practical guide to building web applications with modern Angular',15,1,'Dive into Angular Development — With the Most Trusted Guide in the Industry\r\n\r\nAngular is one of the most powerful and widely adopted JavaScript frameworks, and Learning Angular is your go-to guide for building real-world, production-ready web applications. Written by a seasoned Angular developer and Google Developer Expert, this hands-on book takes you through every step of modern frontend development.\r\n\r\nThis edition reflects the latest “Angular Renaissance,” covering standalone components, Angular Signals, and the new control flow syntax, while showing you how to integrate with legacy code. A new chapter also explores boosting performance with server-side rendering (SSR) and hydration.\r\n\r\nMore than just a tutorial, Learning Angular builds your confidence chapter by chapter from scaffolding your first project to deploying it, with TypeScript best practices throughout.\r\n\r\nWhether you’re new to Angular or sharpening your skills, this book provides a complete path to becoming a productive, future-ready Angular developer. By the end, you’ll be able to build Angular apps from scratch, with clarity, structure, and confidence.'),(9,'Julia Cameron','/images/books/art.jpg','978014312925',2016,'Tarcher','The Artist\'s Way: 30th Anniversary Edition ',13,1,'Since its first publication, The Artist\'s Way phenomena has inspired the genius of Elizabeth Gilbert and millions of readers to embark on a creative journey and find a deeper connection to process and purpose. Julia Cameron\'s novel approach guides readers in uncovering problems areas and pressure points that may be restricting their creative flow and offers techniques to free up any areas where they might be stuck, opening up opportunities for self-growth and self-discovery.\r\n \r\nThe program begins with Cameron’s most vital tools for creative recovery – The Morning Pages, a daily writing ritual of three pages of stream-of-conscious, and The Artist Date, a dedicated block of time to nurture your inner artist. From there, she shares hundreds of exercises, activities, and prompts to help readers thoroughly explore each chapter. She also offers guidance on starting a “Creative Cluster” of fellow artists who will support you in your creative endeavors.\r\n\r\nA revolutionary program for personal renewal, The Artist\'s Way will help get you back on track, rediscover your passions, and take the steps you need to change your life.'),(10,'B','/images/books/1764947517409_os-book.jpg','1234567898765',1999,'2003','A',5,1,'This is a test book');
/*!40000 ALTER TABLE `books` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `borrowers`
--

DROP TABLE IF EXISTS `borrowers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `borrowers` (
  `ID` bigint NOT NULL AUTO_INCREMENT,
  `DATEOFBIRTH` date DEFAULT NULL,
  `EMAIL` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `FIRSTNAME` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `GENDER` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LASTNAME` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `PASSWORDHASH` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `EMAIL` (`EMAIL`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `borrowers`
--

LOCK TABLES `borrowers` WRITE;
/*!40000 ALTER TABLE `borrowers` DISABLE KEYS */;
INSERT INTO `borrowers` VALUES (1,'1995-05-10','alice@example.com','Alice',NULL,'Smith','$2a$10$Gl80GNT5YGIO5FjQneEYoOBcMhU48AZWkRwtIa5oAybbkUBaiNYqi'),(2,'2025-12-05','Smith@library.borrower.com','Smith','Male','Wison','$2a$10$6mM.M95HPV0r5SatMqAvl.DwlCWYp0BPUBYNfrDTn56ofuUr2vPfO'),(3,'2025-12-05','abc@borrower.com','Yu','Male','Zoe','$2a$10$ok8R7dOmlmDeFhKDjbHCIuZSGIL0ynEATevJjIZ6qBy2azEXbC/bi'),(4,'2000-02-02','bob@borrower.com','Aiden','Other','Bob','$2a$10$7tbFfwR7fdqtPw1fiG7Ka.4FFdbK3Ive5vN6c3l7LoGULCoXY3qLq'),(5,'1999-02-20','Harry@borrower.com','harry','Male','Huang','$2a$10$cbK6MVQXd6K2.QivUdEL5uSoRFQTGWxga4oVdn12U1XghP5EbqaW6'),(6,'2025-12-01','ai@borrower.com','Ai','Female','R','$2a$10$uxQhGin31UADzfhSQBsjreek/4g2I4gjmADnmdHfgWGsU75Oh9Iou');
/*!40000 ALTER TABLE `borrowers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `borrowing_records`
--

DROP TABLE IF EXISTS `borrowing_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `borrowing_records` (
  `ID` bigint NOT NULL AUTO_INCREMENT,
  `BORROWDATE` date DEFAULT NULL,
  `DUEDATE` date DEFAULT NULL,
  `RETURNDATE` date DEFAULT NULL,
  `STATUS` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `book_copy_id` bigint DEFAULT NULL,
  `borrower_id` bigint DEFAULT NULL,
  `REQUESTDATE` date DEFAULT NULL,
  `RETURNREQUESTDATE` date DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_borrowing_records_borrower_id` (`borrower_id`),
  KEY `FK_borrowing_records_book_copy_id` (`book_copy_id`),
  CONSTRAINT `FK_borrowing_records_book_copy_id` FOREIGN KEY (`book_copy_id`) REFERENCES `book_copy` (`ID`),
  CONSTRAINT `FK_borrowing_records_borrower_id` FOREIGN KEY (`borrower_id`) REFERENCES `borrowers` (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `borrowing_records`
--

LOCK TABLES `borrowing_records` WRITE;
/*!40000 ALTER TABLE `borrowing_records` DISABLE KEYS */;
INSERT INTO `borrowing_records` VALUES (27,'2025-12-04','2025-12-18',NULL,'BORROWED',10,3,'2025-12-04',NULL),(28,'2025-12-04','2025-12-18',NULL,'RETURN_REQUESTED',16,3,'2025-12-04','2025-12-04'),(29,NULL,NULL,NULL,'INCOMPLETE',11,3,'2025-12-04',NULL),(30,'2025-12-04','2024-01-01',NULL,'INCOMPLETE',9,3,'2025-12-04',NULL),(31,NULL,NULL,NULL,'REQUESTED_BORROW',14,3,'2025-12-04',NULL),(32,NULL,NULL,NULL,'REQUESTED_BORROW',1,3,'2025-12-04',NULL);
/*!40000 ALTER TABLE `borrowing_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `librarians`
--

DROP TABLE IF EXISTS `librarians`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `librarians` (
  `ID` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `EMAIL` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `first_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `last_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `EMAIL` (`EMAIL`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `librarians`
--

LOCK TABLES `librarians` WRITE;
/*!40000 ALTER TABLE `librarians` DISABLE KEYS */;
INSERT INTO `librarians` VALUES (3,'2025-11-29 22:56:05.000000','ray@admin.com','Ray','Wang','$2a$10$LTl3a3PKnVZNSz71WlPIK.qelNU.xIPDCp.IsYZxuifPv70/ELTfS'),(4,'2025-12-03 19:55:34.995058','zulu@librarian.com','Lu','Zu','$2a$10$YgpfbbMo.r8OiOZ54kE6u.RhD4N0JS/3W88.YNJMBQG.e5x1..Hwu');
/*!40000 ALTER TABLE `librarians` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_admins`
--

DROP TABLE IF EXISTS `system_admins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_admins` (
  `ID` bigint NOT NULL AUTO_INCREMENT,
  `EMAIL` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `PASSWORDHASH` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `EMAIL` (`EMAIL`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_admins`
--

LOCK TABLES `system_admins` WRITE;
/*!40000 ALTER TABLE `system_admins` DISABLE KEYS */;
INSERT INTO `system_admins` VALUES (2,'admin@library.com','$2a$10$1QxzAiKQvAT0IDo5YUKXyOWKNefjYDS1TCusCihyZHs0ubsMJTS7O');
/*!40000 ALTER TABLE `system_admins` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-06  0:05:58
