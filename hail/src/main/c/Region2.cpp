#include "hail/Region2.h"
#include "hail/Upcalls.h"
#include <memory>
#include <vector>
#include <utility>
#include <algorithm>

namespace hail {

void RegionPtr::clear() {
  if (region_ != nullptr) {
    --(region_->references_);
    if (region_->references_ == 0) {
      region_->clear();
      region_->pool_->free_regions_.push_back(region_);
    }
    region_ = nullptr;
  }
}

Region2::Region(RegionPool * pool) :
pool_(pool),
block_offset_(0),
current_block_(pool->get_block()) { }

char * Region2::allocate_new_block(size_t n) {
  used_blocks_.push_back(std::move(current_block_));
  current_block_ = pool_->get_block();
  block_offset_ = n;
  return current_block_.get();
}

char * Region2::allocate_big_chunk(size_t n) {
  big_chunks_.push_back(std::make_unique<char[]>(n));
  return big_chunks_.back().get();
}

void Region2::clear() {
  block_offset_ = 0;
  std::move(std::begin(used_blocks_), std::end(used_blocks_), std::back_inserter(pool_->free_blocks_));
  used_blocks_.clear();
  big_chunks_.clear();
  parents_.clear();
}

RegionPtr Region2::get_region() {
  return pool_->get_region();
}

void Region2::add_reference_to(RegionPtr region) {
  parents_.push_back(std::move(region));
}

std::unique_ptr<char[]> RegionPool::get_block() {
  if (free_blocks_.empty()) {
    return std::make_unique<char[]>(block_size);
  }
  std::unique_ptr<char[]> block = std::move(free_blocks_.back());
  free_blocks_.pop_back();
  return block;
}

RegionPtr RegionPool::new_region() {
  regions_.emplace_back(new Region2(this));
  return RegionPtr(regions_.back().get());
}

RegionPtr RegionPool::get_region() {
  if (free_regions_.empty()) {
    return new_region();
  }
  Region2 * region = std::move(free_regions_.back());
  free_regions_.pop_back();
  return RegionPtr(region);
}

}