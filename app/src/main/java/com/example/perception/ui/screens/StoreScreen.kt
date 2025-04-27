package com.example.perception.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.perception.R

data class Product(
    val name: String,
    val price: String,
    val imageRes: Int
)

val categories = listOf("Popular", "Chair", "Table", "Armchair", "Bed", "Lamp")

val products = listOf(
    Product("Black Simple Lamp", "Rs. 1200.00", R.drawable.lamp),
    Product("Minimal Stand", "Rs. 2500.00", R.drawable.stand),
    Product("Coffee Chair", "Rs. 2000.00", R.drawable.chair),
    Product("Simple Desk", "Rs. 5000.00", R.drawable.desk)
)
@Composable
fun StoreScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Best Deals",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        CategoryRow()

        Spacer(modifier = Modifier.height(16.dp))

        // Let LazyVerticalGrid handle the scrolling
        ProductGrid(products)
    }
}

//@Composable
//fun StoreScreen() {
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//            .verticalScroll(rememberScrollState())
//    ) {
//        Text(
//            text = "Make home",
//            fontSize = 16.sp,
//            color = Color.Gray
//        )
//        Text(
//            text = "BEAUTIFUL",
//            fontSize = 24.sp,
//            fontWeight = FontWeight.Bold
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        CategoryRow()
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        ProductGrid(products)
//    }
//}

@Composable
fun CategoryRow() {
    var selectedCategory by remember { mutableStateOf("Popular") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categories.forEach { category ->
            val isSelected = category == selectedCategory
            FilterChip(
                selected = isSelected,
                onClick = { selectedCategory = category },
                label = { Text(category) }
            )
        }
    }
}

//@Composable
//fun ProductGrid(productList: List<Product>) {
//    LazyVerticalGrid(
//        columns = GridCells.Fixed(2),
//        verticalArrangement = Arrangement.spacedBy(12.dp),
//        horizontalArrangement = Arrangement.spacedBy(12.dp),
//        content = {
//            items(productList) { product ->
//                ProductCard(product)
//            }
//        },
//        modifier = Modifier.fillMaxHeight()
//    )
//}
@Composable
fun ProductGrid(productList: List<Product>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = {
            items(productList) { product ->
                ProductCard(product)
            }
        },
        modifier = Modifier
            .fillMaxSize() // This is important for scroll to work correctly
    )
}

@Composable
fun ProductCard(product: Product) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle click */ }
    ) {
        Image(
            painter = painterResource(id = product.imageRes),
            contentDescription = product.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(150.dp)
                .fillMaxWidth()
                .background(Color.LightGray)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = product.name, fontSize = 14.sp)
        Text(text = product.price, fontWeight = FontWeight.Bold)
    }
}